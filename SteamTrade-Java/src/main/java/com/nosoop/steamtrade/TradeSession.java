package com.nosoop.steamtrade;

import bundled.steamtrade.org.json.JSONObject;
import bundled.steamtrade.org.json.JSONException;
import com.nosoop.steamtrade.status.*;
import com.nosoop.steamtrade.TradeListener.TradeStatusCodes;
import com.nosoop.steamtrade.inventory.*;
import com.nosoop.steamtrade.status.TradeEvent.TradeAction;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a session of a trade.
 *
 * @author Top-Cat, nosoop
 */
public class TradeSession implements Runnable {
    /**
     * Static URL properties.
     */
    public final static String STEAM_COMMUNITY_DOMAIN = "steamcommunity.com",
            STEAM_TRADE_URL = "http://steamcommunity.com/trade/%s/";
    /**
     * Object to lock while polling and handling updates.
     */
    protected final Object POLL_LOCK = new Object();
    /**
     * Object representation of the users in the trade.
     */
    private final TradeUser TRADE_USER_SELF, TRADE_USER_PARTNER;
    /**
     * List of app-context pairs for the active client's inventory. (A list of
     * the inventories we have, basically.)
     */
    public List<AppContextPair> myAppContextData;
    /**
     * Collection of methods to interact with the current trade session.
     */
    private final TradeCommands API;
    /**
     * String values needed for the trade.
     */
    private final String TRADE_URL, STEAM_LOGIN, SESSION_ID;
    /**
     * Status values.
     */
    public Status status = null;
    protected int version = 1, logpos;
    int lastEvent = 0;
    /**
     * A TradeListener instance that listens for events fired by this session.
     */
    private TradeListener tradeListener;
    /**
     * Timing variables to check for idle state.
     */
    private final long TIME_TRADE_START;
    private long timeLastPartnerAction;

    /**
     * Creates a new trading session.
     *
     * @param steamidSelf Long representation of our own SteamID.
     * @param steamidPartner Long representation of our trading partner's
     * SteamID.
     * @param sessionId String value of the Base64-encoded session token.
     * @param token String value of Steam's login token.
     * @param listener Trade listener to respond to trade actions.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public TradeSession(long steamidSelf, long steamidPartner, String sessionId,
            String token, TradeListener listener) {
        this(steamidSelf, steamidPartner, sessionId, token, listener,
                new ArrayList<AssetBuilder>());
    }

    /**
     * Creates a new trading session.
     *
     * @param steamidSelf Long representation of our own SteamID.
     * @param steamidPartner Long representation of our trading partner's
     * SteamID.
     * @param sessionId String value of the Base64-encoded session token.
     * @param token String value of Steam's login token.
     * @param listener Trade listener to respond to trade actions.
     * @param assetBuilders An integer-to-assetbuilder map. The integers refer
     * to the appid of the inventory to be modified.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public TradeSession(long steamidSelf, long steamidPartner, String sessionId,
            String token, TradeListener listener,
            final List<AssetBuilder> assetBuilders) {
        SESSION_ID = sessionId;
        STEAM_LOGIN = token;

        tradeListener = listener;
        tradeListener.trade = this;

        TRADE_USER_SELF = new TradeUser(steamidSelf, assetBuilders);
        TRADE_USER_PARTNER = new TradeUser(steamidPartner, assetBuilders);

        TRADE_URL = String.format(STEAM_TRADE_URL, steamidPartner);
        API = new TradeCommands();

        tradeListener.onWelcome();
        scrapeBackpackContexts();

        tradeListener.onAfterInit();

        timeLastPartnerAction = TIME_TRADE_START = System.currentTimeMillis();
    }

    /**
     * Polls the TradeSession for updates. Suggested poll rate is once every
     * second.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        synchronized (POLL_LOCK) {
            try {
                status = API.getStatus();
            } catch (final JSONException e) {
                tradeListener.onError(
                        TradeStatusCodes.STATUS_PARSE_ERROR, e.getMessage());

                try {
                    API.cancelTrade();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                tradeListener.onTradeClosed();
            }

            // Update version
            if (status.newversion) {
                version = status.version;
            }

            if (lastEvent < status.events.size()) {
                // Process all new, unhandled events.
                for (; lastEvent < status.events.size(); lastEvent++) {
                    handleTradeEvent(status.events.get(lastEvent));
                }
            } else {
                // If there was no new action during this poll, update timer.
                final long timeCurrent = System.currentTimeMillis();

                final int secondsSinceLastAction =
                        (int) (timeCurrent - timeLastPartnerAction) / 1000;
                final int secondsSinceTradeStart =
                        (int) (timeCurrent - TIME_TRADE_START) / 1000;

                tradeListener.onTimer(
                        secondsSinceLastAction, secondsSinceTradeStart);
            }

            if (status.trade_status == TradeStatusCodes.TRADE_COMPLETED) {
                // Trade successful.
                tradeListener.onTradeSuccess();
                tradeListener.onTradeClosed();
            } else if (status.trade_status
                    == TradeStatusCodes.STATUS_ERRORMESSAGE) {
                tradeListener.onError(status.trade_status, status.error);
                tradeListener.onTradeClosed();
            } else if (status.trade_status > 1) {
                // Refer to TradeListener.TradeStatusCodes for known values.
                tradeListener.onError(status.trade_status,
                        TradeStatusCodes.EMPTY_MESSAGE);
                tradeListener.onTradeClosed();
            }

            // Update Local Variables
            if (status.them != null) {
                TRADE_USER_PARTNER.ready = status.them.ready;
                TRADE_USER_SELF.ready = status.me.ready;
            }

            // Update version
            if (status.newversion) {
                tradeListener.onNewVersion();
            }

            if (status.logpos != 0) {
                // ... no idea.
                // DebugPrint.println("WAT");
                logpos = status.logpos;
            }
        }
    }

    /**
     * Handles received trade events and fires the appropriate event at the
     * given TradeListener, defined in the constructor.
     *
     * @param evt Trade event being handled.
     */
    private void handleTradeEvent(final TradeEvent evt) {
        // Drop the event if the event's steamid is not theirs.
        boolean isBot = !evt.steamid.equals(
                String.valueOf(TRADE_USER_PARTNER.STEAM_ID));

        // TODO Link their asset to variable item count.
        if (status.them.assets != null) {
            /*
             * System.out.println(
             * java.util.Arrays.toString(status.them.assets.toArray()));
             */
        }

        switch (evt.action) {
            case TradeAction.ITEM_ADDED:
                eventUserAddedItem(evt);
                break;
            case TradeAction.ITEM_REMOVED:
                eventUserRemovedItem(evt);
                break;
            case TradeAction.READY_TOGGLED:
                if (!isBot) {
                    TRADE_USER_PARTNER.ready = true;
                    tradeListener.onUserSetReadyState(true);
                } else {
                    TRADE_USER_SELF.ready = true;
                }
                break;
            case TradeAction.READY_UNTOGGLED:
                if (!isBot) {
                    TRADE_USER_PARTNER.ready = false;
                    tradeListener.onUserSetReadyState(false);
                } else {
                    TRADE_USER_SELF.ready = false;
                }
                break;
            case TradeAction.TRADE_ACCEPTED:
                if (!isBot) {
                    tradeListener.onUserAccept();
                }
                break;
            case TradeAction.MESSAGE_ADDED:
                if (!isBot) {
                    tradeListener.onMessage(evt.text);
                }
                break;
            case 6:
                eventUserSetCurrencyAmount(evt);
                break;
            case 8:
            // TODO Add support for stackable items.
            default:
                // Let the trade listener handle it.
                tradeListener.onUnknownAction(evt);
                break;
        }

        if (!isBot) {
            timeLastPartnerAction = System.currentTimeMillis();
        }
    }

    private void eventUserAddedItem(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(
                String.valueOf(TRADE_USER_PARTNER.STEAM_ID));

        if (!isBot) {
            /**
             * If this is the other user and we don't have their inventory yet,
             * then we will load it.
             */
            TradeInternalInventory userInv;
            TradeInternalItem item;
            do {
                userInv = API.loadForeignInventory(
                        new AppContextPair(evt.appid, evt.contextid));
                item = userInv.getItem(evt.assetid);
            } while (item == null && userInv.hasMore());

            if (item != null) {
                tradeListener.onUserAddItem(item);
            } else {
                // If null after loading the inventory, something's fishy.
                String errorMsg = "Could not load item asset %d in inventory "
                        + "with appid %d and contextid %d.";

                tradeListener.onError(TradeStatusCodes.USER_ITEM_NOT_FOUND,
                        String.format(errorMsg, evt.assetid, evt.appid,
                        evt.contextid));
            }
        }

        // Add to internal tracking.
        final TradeInternalInventories inv = (isBot
                ? TRADE_USER_SELF : TRADE_USER_PARTNER).getInventories();

        final TradeInternalItem item =
                inv.getInventory(evt.appid, evt.contextid).getItem(evt.assetid);

        (isBot ? TRADE_USER_SELF : TRADE_USER_PARTNER).getOffer().add(item);
    }

    private void eventUserRemovedItem(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(
                String.valueOf(TRADE_USER_PARTNER.STEAM_ID));

        if (!isBot) {
            final TradeInternalItem item = TRADE_USER_PARTNER.getInventories()
                    .getInventory(evt.appid, evt.contextid)
                    .getItem(evt.assetid);
            tradeListener.onUserRemoveItem(item);
        }

        // Get the item from one of our inventories and remove.
        final TradeInternalItem item =
                (isBot ? TRADE_USER_SELF : TRADE_USER_PARTNER).getInventories()
                .getInventory(evt.appid, evt.contextid).getItem(evt.assetid);

        (isBot ? TRADE_USER_SELF : TRADE_USER_PARTNER).getOffer().remove(item);
    }

    private void eventUserSetCurrencyAmount(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(
                String.valueOf(TRADE_USER_PARTNER.STEAM_ID));

        if (!isBot) {
            /**
             * If this is the other user and we don't have their inventory yet,
             * then we will load it.
             *
             * Keep loading the rest of the partial inventory until the item is
             * found.
             */
            TradeInternalInventory userInv;
            TradeInternalCurrency item;
            do {
                userInv = API.loadForeignInventory(
                        new AppContextPair(evt.appid, evt.contextid));
                item = userInv.getCurrency(evt.assetid);
            } while (item == null && userInv.hasMore());

            if (item != null) {
                tradeListener.onUserAddItem(item);
            }
        }

        // Add to internal tracking.
        final TradeInternalInventories inv = (isBot
                ? TRADE_USER_SELF : TRADE_USER_PARTNER).getInventories();

        final TradeInternalCurrency item =
                inv.getInventory(evt.appid, evt.contextid)
                .getCurrency(evt.currencyid);

        (isBot ? TRADE_USER_SELF : TRADE_USER_PARTNER).getOffer().add(item);
    }

    /**
     * Loads a copy of the trade screen, passing the data to ContextScraper to
     * generate a list of AppContextPairs to load our inventories with.
     */
    private void scrapeBackpackContexts() {
        // I guess we're scraping the trade page.
        final Map<String, String> data = new HashMap<>();

        String pageData = API.fetch(TRADE_URL, "GET", data);

        try {
            List<AppContextPair> contexts =
                    ContextScraper.scrapeContextData(pageData);
            myAppContextData = contexts;
        } catch (JSONException e) {
            // Notify the trade listener if we can't get our backpack data.
            myAppContextData = new ArrayList<>();
            tradeListener.onError(TradeStatusCodes.BACKPACK_SCRAPE_ERROR,
                    TradeStatusCodes.EMPTY_MESSAGE);

        }
    }

    /**
     * Loads one of our game inventories, storing it in a
     * TradeInternalInventories object.
     *
     * @param appContext An AppContextPair representing the inventory to be
     * loaded.
     */
    public void loadOwnInventory(AppContextPair appContext) {
        final String url, response;

        if (TRADE_USER_SELF.getInventories().hasInventory(appContext)) {
            return;
        }

        // TODO Add support for large inventories ourselves.
        url = String.format(TradeCommands.LOCAL_INVENTORY_FORMAT_URL,
                TRADE_USER_SELF.STEAM_ID,
                appContext.getAppid(), appContext.getContextid());

        response = API.fetch(url, "GET", null, true);

        try {
            JSONObject repsonseObject = new JSONObject(response);
            TRADE_USER_SELF.getInventories()
                    .addInventory(appContext, repsonseObject);
        } catch (JSONException e) {
            tradeListener.onError(TradeStatusCodes.OWN_INVENTORY_LOAD_ERROR,
                    e.getMessage());
        }
    }

    /**
     * Returns our client's Steam ID.
     *
     * @return The client's Steam ID as a 64-bit long value.
     */
    public long getOwnSteamId() {
        return TRADE_USER_SELF.STEAM_ID;
    }

    /**
     * Returns our trading partner's Steam ID.
     *
     * @return The trading partner's Steam ID as a 64-bit long value.
     */
    public long getPartnerSteamId() {
        return TRADE_USER_PARTNER.STEAM_ID;
    }

    /**
     * Returns a TradeUser instance containing our Steam ID, loaded inventories,
     * offer, and whether or not we are ready.
     *
     * @return A TradeUser instance containing data for the running client.
     */
    public TradeUser getSelf() {
        return TRADE_USER_SELF;
    }

    /**
     * Returns a TradeUser instance containing our trading partner's Steam ID,
     * loaded inventories, offer, and whether or not they are ready.
     *
     * @return A TradeUser instance containing data for the trading partner.
     */
    public TradeUser getPartner() {
        return TRADE_USER_PARTNER;
    }

    /**
     * Gets the commands associated with this trade session.
     *
     * @return TradeCommands object that handles the user-trade actions.
     */
    public TradeCommands getCmds() {
        return API;
    }

    /**
     * A utility class to hold all web-based 'fetch' actions when dealing with
     * Steam Trade in the current trading session.
     *
     * @author nosoop
     */
    public class TradeCommands {
        /**
         * The user-agent string to use when making requests.
         */
        final static String USER_AGENT = "SteamTrade-Java/1.0 "
                + "(Windows; U; Windows NT 6.1; en-US; "
                + "Valve Steam Client/1392853084; SteamTrade-Java Client; ) "
                + "AppleWebKit/535.19 (KHTML, like Gecko) "
                + "Chrome/18.0.1025.166 Safari/535.19";
        /**
         * The format string representing a URL to load our client's inventory.
         * The three placeholders are for STEAMID, APPID, and CONTEXTID.
         */
        final static String LOCAL_INVENTORY_FORMAT_URL =
                "http://steamcommunity.com/profiles/%d/"
                + "inventory/json/%d/%d/?trading=1";
        /**
         * A URL-decoded copy of SESSION_ID. Needed to make requests.
         */
        final String DECODED_SESSION_ID;

        /**
         * Initializes the instance and attempts to create a URL-decoded copy of
         * the SESSION_ID. Fails if the system does not support UTF-8.
         */
        TradeCommands() {
            try {
                DECODED_SESSION_ID = URLDecoder.decode(SESSION_ID, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                /**
                 * If you can't decode to UTF-8, well, you're kinda boned. No
                 * way to get around the issue, I think, so we'll just throw an
                 * error.
                 */
                throw new Error(e);
            }
        }

        /**
         * Tell the trading service to add one of our own items into the trade.
         *
         * @param item The item, represented by an TradeInternalItem instance.
         * @param slot The offer slot to place the item in (0~255).
         */
        public void addItem(TradeInternalItem item, int slot) {
            addItem(item.getAppid(), item.getContextid(), item.getAssetid(),
                    slot);
        }

        /**
         * Adds an item to the trade directly, as opposed to using a
         * TradeInternalItem instance.
         *
         * @param appid The game inventory for the item.
         * @param contextid The inventory "context" for the item.
         * @param assetid The inventory "asset", the item id.
         * @param slot The offer slot to place the item in (0~255).
         */
        public void addItem(int appid, long contextid, long assetid, int slot) {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("appid", "" + appid);
            data.put("contextid", "" + contextid);
            data.put("itemid", "" + assetid);
            data.put("slot", "" + slot);

            fetch(TRADE_URL + "additem", "POST", data);
        }

        /**
         * Removes an item from the trade.
         *
         * @param item The item, represented by an TradeInternalItem instance.
         */
        public void removeItem(TradeInternalItem item) {
            removeItem(item.getAppid(), item.getContextid(), item.getAssetid());
        }

        /**
         * Removes an item from the trade directly, as opposed to using a
         * TradeInternalItem instance.
         *
         * @param appid The game inventory for the item.
         * @param contextid The inventory "context" for the item.
         * @param assetid The inventory "asset", the item id.
         */
        public void removeItem(int appid, long contextid, long assetid) {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("appid", "" + appid);
            data.put("contextid", "" + contextid);
            data.put("itemid", "" + assetid);

            fetch(TRADE_URL + "removeitem", "POST", data);
        }

        /**
         * Tick / untick the checkbox signaling that we are ready to complete
         * the trade.
         *
         * @param ready Whether the client is ready to trade or not
         * @return True on success, false otherwise.
         */
        public boolean setReady(boolean ready) {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("ready", ready ? "true" : "false");
            data.put("version", "" + version);

            final String response =
                    fetch(TRADE_URL + "toggleready", "POST", data);

            try {
                Status readyStatus = new Status(new JSONObject(response));
                if (readyStatus.success) {
                    if (readyStatus.trade_status
                            == TradeStatusCodes.STATUS_OK) {
                        TRADE_USER_PARTNER.ready = readyStatus.them.ready;
                        TRADE_USER_SELF.ready = readyStatus.me.ready;
                    } else {
                        TRADE_USER_SELF.ready = true;
                    }
                    return TRADE_USER_SELF.ready;
                }
            } catch (final JSONException e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * Hits the "Make Trade" button, finalizing the trade. Not sure what the
         * response is for.
         *
         * @return JSONObject representing trade status.
         * @throws JSONException if the response is unexpected.
         */
        public JSONObject acceptTrade() throws JSONException {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("version", "" + version);

            final String response = fetch(TRADE_URL + "confirm", "POST", data);

            return new JSONObject(response);
        }

        /**
         * Cancels the trade session as if we clicked the "Cancel Trade" button.
         * Expect a call of onError(TradeErrorCodes.TRADE_CANCELLED).
         *
         * @return True if server responded as successful, false otherwise.
         * @throws JSONException when there is an error in parsing the response.
         */
        public boolean cancelTrade() throws JSONException {
            final Map<String, String> data = new HashMap();
            data.put("sessionid", DECODED_SESSION_ID);
            final String response = fetch(TRADE_URL + "cancel", "POST", data);

            return (new JSONObject(response)).getBoolean("success");
        }

        /**
         * Adds a message to trade chat.
         *
         * @param message The message to add to trade chat.
         * @return String representing server response
         */
        public String sendMessage(String message) {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("message", message);
            data.put("logpos", "" + logpos);
            data.put("version", "" + version);

            return fetch(TRADE_URL + "chat", "POST", data);
        }

        /**
         * Fetches status updates to the current trade.
         *
         * @return Status object to be processed.
         * @throws JSONException Malformed / invalid response data.
         */
        private Status getStatus() throws JSONException {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("logpos", "" + logpos);
            data.put("version", "" + version);

            final String response = fetch(TRADE_URL + "tradestatus/", "POST",
                    data);

            return new Status(new JSONObject(response));
        }

        /**
         * Loads a foreign inventory. If the inventory already exists partially
         * loaded, continue loading the inventory.
         *
         * @param appContext
         * @return
         */
        public synchronized TradeInternalInventory loadForeignInventory(
                AppContextPair appContext) {
            final Map<String, String> data = new HashMap<>();
            data.put("sessionid", DECODED_SESSION_ID);
            data.put("steamid", TRADE_USER_PARTNER.STEAM_ID + "");
            data.put("appid", appContext.getAppid() + "");
            data.put("contextid", appContext.getContextid() + "");


            if (!TRADE_USER_PARTNER.INVENTORIES.hasInventory(appContext)) {
                /**
                 * Make a nonexistent inventory if needed.
                 */
                TRADE_USER_PARTNER.INVENTORIES.addInventory(appContext);
            }

            TradeInternalInventory inventory =
                    TRADE_USER_PARTNER.INVENTORIES.getInventory(appContext);

            if (inventory.getMoreStartPosition() != 0 || inventory.hasMore()) {
                data.put("start", inventory.getMoreStartPosition() + "");
            }


            try {
                String feed = fetch(TRADE_URL + "foreigninventory/", "GET",
                        data);

                JSONObject jsonData = new JSONObject(feed);

                inventory.loadMore(jsonData);
                return inventory;
            } catch (JSONException e) {
                // Something wrong happened...
                return inventory;
            }
        }

        /**
         * Requests a String representation of an online file.
         *
         * @param url Location to fetch.
         * @param method "GET" or "POST"
         * @param data The data to be added to the data stream or request
         * params.
         * @return The server's String response to the request.
         */
        String fetch(String url, String method, Map<String, String> data) {
            return fetch(url, method, data, true);
        }

        /**
         * Requests a String representation of an online file (for Steam).
         *
         * @param url Location to fetch.
         * @param method "GET" or "POST"
         * @param data The data to be added to the data stream or request
         * params.
         * @param sendLoginData Whether or not to send login session data.
         * @return The server's String response to the request.
         */
        String fetch(String url, String method, Map<String, String> data,
                boolean sendLoginData) {
            String cookies = "";
            if (sendLoginData) {
                cookies = "sessionid=" + DECODED_SESSION_ID + "; "
                        + "steamLogin=" + STEAM_LOGIN + ";";
            }
            final String response = request(url, method, data, cookies);
            return response;
        }

        /**
         * Requests a String representation of an online file (for Steam).
         *
         * @param url Location to fetch.
         * @param method "GET" or "POST"
         * @param data The data to be added to the data stream or request
         * params.
         * @param cookies A string of cookie data to be added to the request
         * headers.
         * @return The server's String response to the request.
         */
        String request(String url, String method, Map<String, String> data,
                String cookies) {
            boolean ajax = true;
            StringBuilder out = new StringBuilder();
            try {
                String dataString;
                StringBuilder dataStringBuffer = new StringBuilder();
                if (data != null) {
                    for (Map.Entry<String, String> entry : data.entrySet()) {
                        dataStringBuffer.append(
                                URLEncoder.encode(entry.getKey(), "UTF-8"))
                                .append("=").append(
                                URLEncoder.encode(entry.getValue(), "UTF-8"))
                                .append("&");
                        /**
                         * dataString += URLEncoder.encode( entry.getKey(),
                         * "UTF-8") + "=" + URLEncoder.encode( entry.getValue(),
                         * "UTF-8") + "&";
                         */
                    }
                }
                dataString = dataStringBuffer.toString();
                if (!method.equals("POST")) {
                    url += "?" + dataString;
                }
                final URL url2 = new URL(url);
                final HttpURLConnection conn =
                        (HttpURLConnection) url2.openConnection();
                conn.setRequestProperty("Cookie", cookies);
                conn.setRequestMethod(method);
                System.setProperty("http.agent", "");

                conn.setRequestProperty("User-Agent", USER_AGENT);

                conn.setRequestProperty("Host", "steamcommunity.com");
                conn.setRequestProperty("Content-type",
                        "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("Accept",
                        "text/javascript, text/hml, "
                        + "application/xml, text/xml, */*");

                /**
                 * Turns out we need a referer, otherwise we get an error from
                 * the server. Just use the trade URL as one since we have it on
                 * hand, and it's been known to work.
                 */
                conn.setRequestProperty("Referer", TRADE_URL);

                // Accept compressed responses.  (We can decompress it.)
                conn.setRequestProperty("Accept-Encoding", "gzip,deflate");

                if (ajax) {
                    conn.setRequestProperty("X-Requested-With",
                            "XMLHttpRequest");
                    conn.setRequestProperty("X-Prototype-Version", "1.7");
                }

                if (method.equals("POST")) {
                    conn.setDoOutput(true);
                    try (OutputStreamWriter os =
                            new OutputStreamWriter(conn.getOutputStream())) {
                        os.write(dataString.substring(0,
                                dataString.length() - 1));
                        os.flush();
                    }
                }

                java.io.InputStream netStream = conn.getInputStream();

                // If GZIPped response, then use the gzip decoder.
                if (conn.getContentEncoding().contains("gzip")) {
                    netStream = new java.util.zip.GZIPInputStream(netStream);
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(netStream))) {
                    String line; // Stores the currently read line.
                    while ((line = reader.readLine()) != null) {
                        if (out.length() > 0) {
                            out.append('\n');
                        }
                        out.append(line);
                    }
                    reader.close();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
            return out.toString();
        }
    }

    /**
     * A set of values associated with one of the two users currently in the
     * trade.
     *
     * @author nosoop
     */
    public static class TradeUser {
        final long STEAM_ID;
        final Set<TradeInternalAsset> TRADE_OFFER;
        final TradeInternalInventories INVENTORIES;
        boolean ready;

        TradeUser(long steamid, List<AssetBuilder> assetBuilders) {
            this.STEAM_ID = steamid;
            this.TRADE_OFFER = new HashSet<>();
            this.INVENTORIES = new TradeInternalInventories(assetBuilders);
            this.ready = false;
        }

        /**
         * @return A set of TradeInternalAsset instances displaying the offer,
         * containing TradeInternalItem and TradeInternalCurrency instances.
         */
        public Set<TradeInternalAsset> getOffer() {
            return TRADE_OFFER;
        }

        /**
         * @return A 64-bit long representation of the instance Steam ID.
         */
        public long getSteamID() {
            return STEAM_ID;
        }

        /**
         * @return The selected user's TradeInternalInventories instance.
         */
        public TradeInternalInventories getInventories() {
            return INVENTORIES;
        }

        /**
         * @return Whether or not the selected user is ready.
         */
        public boolean isReady() {
            return ready;
        }
    }

}

/**
 * Private class that brutally scrapes the AppContextData JavaScript object from
 * the trade page. Without this, we would not know what inventories we have.
 *
 * @author nosoop
 */
class ContextScraper {
    // TODO Uncouple this from the TradeSession class?
    static final List<AppContextPair> DEFAULT_APPCONTEXTDATA =
            new ArrayList<>();

    /**
     * Initialize default AppContextPairs.
     */
    static {
        DEFAULT_APPCONTEXTDATA.add(
                new AppContextPair(440, 2, "Team Fortress 2"));
    }

    /**
     * Scrapes the page for the g_rgAppContextData variable and passes it to a
     * private method for parsing, returning the list of named AppContextPair
     * objects it generates. It's a bit of a hack...
     *
     * @param pageResult The page data fetched by the TradeSession object.
     * @return A list of named AppContextPair objects representing the known
     * inventories, or an empty list if not found.
     */
    static List<AppContextPair> scrapeContextData(String pageResult)
            throws JSONException {
        try {
            BufferedReader read;
            read = new BufferedReader(new StringReader(pageResult));

            String buffer;
            while ((buffer = read.readLine()) != null) {
                String input;
                input = buffer.trim();

                if (input.startsWith("var g_rgAppContextData")) {
                    // Extract the JSON string from the JavaScript source.  Bleh
                    return parseContextData(input.substring(input.indexOf('{'),
                            input.length() - 1));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // If we can't find it, return an empty one, I guess...
        return DEFAULT_APPCONTEXTDATA;
    }

    /**
     * Parses the context data JSON feed and makes a bunch of AppContextPair
     * instances.
     *
     * @param json The JSON String representing g_rgAppContextData.
     * @return A list of named AppContextPair objects representing the available
     * inventories.
     */
    private static List<AppContextPair> parseContextData(String json)
            throws JSONException {
        List<AppContextPair> result = new ArrayList<>();

        JSONObject feedData = new JSONObject(json);

        for (String on : (Set<String>) feedData.keySet()) {
            JSONObject o = feedData.getJSONObject(on);
            if (o != null) {
                String gameName = o.getString("name");
                int appid = o.getInt("appid");

                JSONObject contextData = o.getJSONObject("rgContexts");

                for (String bn : (Set<String>) contextData.keySet()) {
                    JSONObject b = contextData.getJSONObject(bn);
                    String contextName = b.getString("name");
                    long contextid = Long.parseLong(b.getString("id"));
                    int assetCount = b.getInt("asset_count");

                    // "Team Fortress 2 - Backpack (226)"
                    String invNameFormat = String.format("%s - %s (%d)",
                            gameName, contextName, assetCount);

                    // Only include the inventory if it's not empty.
                    if (assetCount > 0) {
                        result.add(new AppContextPair(
                                appid, contextid, invNameFormat));
                    }
                }
            }
        }
        return result;
    }
}
