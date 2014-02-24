package com.nosoop.steamtrade;

import com.nosoop.steamtrade.status.*;
import com.nosoop.steamtrade.TradeListener.TradeErrorCodes;
import com.nosoop.steamtrade.inventory.*;
import com.nosoop.steamtrade.status.TradeEvent.TradeAction;
import java.io.UnsupportedEncodingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/**
 * Represents a session of a trade.
 *
 * @author Top-Cat, nosoop
 */
public class TradeSession implements Runnable {

    // Static properties
    public final static String STEAM_COMMUNITY_DOMAIN = "steamcommunity.com";
    public final static String STEAM_TRADE_URL = "http://steamcommunity.com/trade/%s/";
    // Generic Trade info
    public boolean meReady = false, otherReady = false;
    boolean tradeStarted = false;
    int lastEvent = 0;
    public final Object pollLock = new Object();
    //
    // The items put up for offer.
    // TODO Make this my/other stuff into a struct.
    public Set<TradeInternalItem> myTradeOffer = new HashSet<>(), otherTradeOffer = new HashSet<>();
    public Object[] trades;
    //
    // The inventories of both users.
    public TradeInternalInventories otherUserTradeInventories, myTradeInventories;
    public List<AppContextPair> myAppContextData;
    //
    // Trade interfacing object.
    private TradeCommands api;
    //
    // Internal properties needed for Steam API.
    protected String baseTradeURL;
    protected String steamLogin;
    protected String sessionId;
    protected int version = 1;
    protected int logpos;
    protected int numEvents;
    //
    // The trade listener to offload events to.
    public TradeListener tradeListener;
    //
    // Timing variables.
    private long timeTradeStarted, timeLastAction;
    private long steamIdSelf, steamIdPartner;

    /**
     * Starts a new trading session.
     *
     * @param steamidSelf Long representation of our own SteamID.
     * @param steamidPartner Long representation of our trading partner's
     * SteamID.
     * @param sessionId String value of the Base64-encoded session token.
     * @param token String value of Steam's login token.
     * @param listener Trade listener to respond to trade actions.
     * @throws Exception
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public TradeSession(long steamidSelf, long steamidPartner, String sessionId, String token, TradeListener listener) {
        steamIdSelf = steamidSelf;
        steamIdPartner = steamidPartner;

        trades = new Object[]{myTradeOffer, otherTradeOffer};

        this.sessionId = sessionId;
        steamLogin = token;

        listener.trade = this;
        tradeListener = listener;

        baseTradeURL = String.format(TradeSession.STEAM_TRADE_URL, steamIdPartner);

        api = new TradeCommands(baseTradeURL, this.sessionId, steamLogin);

        myTradeInventories = new TradeInternalInventories();
        otherUserTradeInventories = new TradeInternalInventories();

        tradeListener.onWelcome();
        scrapeBackpackContexts();

        tradeListener.onAfterInit();
    }
    public Status status = null;

    /**
     * Polls the TradeSession for updates.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        synchronized (pollLock) {
            if (!tradeStarted) {
                tradeStarted = true;

                timeTradeStarted = System.currentTimeMillis();
                timeLastAction = System.currentTimeMillis();
            }

            try {
                status = getStatus();
            } catch (final ParseException e) {
                e.printStackTrace();
                tradeListener.onError(1);
                return;
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

                final int secondsSinceLastAction = (int) ((timeCurrent - timeLastAction) / 1000);
                final int secondsSinceTradeStart = (int) ((timeCurrent - timeTradeStarted) / 1000);

                tradeListener.onTimer(secondsSinceLastAction, secondsSinceTradeStart);
            }

            if (status.trade_status == 3) {
                // One trader cancelled.  (Can't determine who from the status.)
                fireEventError(TradeErrorCodes.TRADE_CANCELLED);
            } else if (status.trade_status == 4) {
                // Other user timed out according to trade system.
                fireEventError(TradeErrorCodes.PARTNER_TIMED_OUT);
            } else if (status.trade_status == 5) {
                // Trade failed.
                fireEventError(TradeErrorCodes.TRADE_FAILED);
            } else if (status.trade_status == 1) {
                // Trade successful.
                tradeListener.onTradeSuccess();
                tradeListener.onTradeClosed();
            }

            // Update Local Variables
            if (status.them != null) {
                otherReady = status.them.ready;
                meReady = status.me.ready;
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
     * TradeListener defined in the constructor.
     *
     * @param evt Trade event being handled.
     */
    private void handleTradeEvent(final TradeEvent evt) {
        // Drop the event if the event's steamid is not theirs.
        boolean isBot = !evt.steamid.equals(String.valueOf(steamIdPartner));

        // TODO Link their asset to variable item count.
        if (status.them.assets != null) {
            //System.out.println(Arrays.toString(status.them.assets.toArray()));
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
                    otherReady = true;
                    tradeListener.onUserSetReadyState(true);
                } else {
                    meReady = true;
                }
                break;
            case TradeAction.READY_UNTOGGLED:
                if (!isBot) {
                    otherReady = false;
                    tradeListener.onUserSetReadyState(false);
                } else {
                    meReady = false;
                }
                break;
            case 4:
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
            // TODO Add support for currency.
            //break;
            case 8:
            // TODO Add support for stackable items.
            //break;
            default:
                // DebugPrint.println("Unknown Event ID: " + evt.action);
                System.out.println(evt.getJSONObject());
                break;
        }

        if (!isBot) {
            timeLastAction = System.currentTimeMillis();
        }
    }

    private void eventUserAddedItem(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(String.valueOf(steamIdPartner));

        if (!isBot) {
            if (!otherUserTradeInventories.hasInventory(evt.appid, evt.contextid)) {
                addForeignInventory(steamIdPartner, evt.appid, evt.contextid);
            }
            final TradeInternalItem item = otherUserTradeInventories.getInventory(evt.appid, evt.contextid).getItem(evt.assetid);
            tradeListener.onUserAddItem(item);
        }

        // Add to internal tracking.
        final TradeInternalInventories inv = isBot
                ? myTradeInventories : otherUserTradeInventories;

        final TradeInternalItem item =
                inv.getInventory(evt.appid, evt.contextid).getItem(evt.assetid);

        ((Set<TradeInternalItem>) trades[isBot ? 0 : 1]).add(item);
    }

    private void eventUserRemovedItem(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(String.valueOf(steamIdPartner));
        ((Set<Long>) trades[isBot ? 0 : 1]).remove(evt.assetid);
        if (!isBot) {
            final TradeInternalItem item = otherUserTradeInventories.getInventory(evt.appid, evt.contextid).getItem(evt.assetid);
            tradeListener.onUserRemoveItem(item);
        }

        // Get the item from one of our inventories and remove.
        final TradeInternalItem item =
                (isBot ? myTradeInventories : otherUserTradeInventories)
                .getInventory(evt.appid, evt.contextid).getItem(evt.assetid);

        ((Set<TradeInternalItem>) trades[isBot ? 0 : 1]).remove(item);
    }

    private void eventUserSetCurrencyAmount(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(String.valueOf(steamIdPartner));
        // TODO Set support for currency?
        if (!isBot) {
        }
    }

    private void fireEventError(int errorCode) {
        tradeListener.onError(errorCode);
        tradeListener.onTradeClosed();
    }

    /**
     * Loads a copy of the trade screen, passing the data to ContextScraper to
     * generate a list of AppContextPairs as reference to load inventories with.
     */
    private void scrapeBackpackContexts() {
        // I guess we're scraping the trade page.
        final Map<String, String> data = new HashMap<>();

        String pageData = api.fetch(baseTradeURL, "GET", data);
        List<AppContextPair> contexts = ContextScraper.scrapeContextData(pageData);

        myAppContextData = contexts;
    }

    protected Status getStatus() throws ParseException {
        final Map<String, String> data = new HashMap<>();
        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("logpos", "" + logpos);
        data.put("version", "" + version);

        final String response = api.fetch(baseTradeURL + "tradestatus/", "POST", data);

        return new Status((JSONObject) new JSONParser().parse(response));
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

        if (myTradeInventories.hasInventory(appContext)) {
            return;
        }

        url = String.format("http://steamcommunity.com/profiles/%d/inventory/json/%d/%d/?trading=1", steamIdSelf, appContext.getAppid(), appContext.getContextid());

        response = api.fetch(url, "GET", null, true);

        myTradeInventories.addInventory(appContext, response);
    }

    /**
     * Loads a copy of the other person's possibly private inventory, once we
     * receive an item from it.
     *
     * @param otherId
     * @param appId
     * @param contextId
     */
    protected synchronized void addForeignInventory(long otherId, int appId, long contextId) {
        final Map<String, String> data = new HashMap<>();

        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("steamid", otherId + "");
        data.put("appid", appId + "");
        data.put("contextid", contextId + "");

        String feed = api.fetch(baseTradeURL + "foreigninventory", "POST", data);

        otherUserTradeInventories.addInventory(appId, contextId, feed);
    }

    /**
     * Gets the commands associated with this trade session.
     *
     * @return TradeCommands object that handles the user-trade actions.
     */
    public TradeCommands getCmds() {
        return api;
    }

    /**
     * A utility class to hold all web-based 'fetch' actions when dealing with
     * Steam Trade.
     *
     * @author nosoop
     */
    public class TradeCommands {

        final String baseTradeURL;
        final String sessionId;
        final String steamLogin;

        TradeCommands(String baseTradeURL, String sessionId, String steamLogin) {
            this.baseTradeURL = baseTradeURL;
            this.sessionId = sessionId;
            this.steamLogin = steamLogin;
        }

        public void addItem(TradeInternalItem item, int slot) {
            addItem(item.appid, item.contextid, item.assetid, slot);
        }

        public void addItem(int appid, long contextid, long assetid, int slot) {
            final Map<String, String> data = new HashMap<>();

            try {
                data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            data.put("appid", "" + appid);
            data.put("contextid", "" + contextid);
            data.put("itemid", "" + assetid);
            data.put("slot", "" + slot);
            fetch(baseTradeURL + "additem", "POST", data);
        }

        public void removeItem(TradeInternalItem item) {
            removeItem(item.appid, item.contextid, item.assetid);
        }

        public void removeItem(int appid, long contextid, long assetid) {
            final Map<String, String> data = new HashMap<>();

            try {
                data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            data.put("appid", "" + appid);
            data.put("contextid", "" + contextid);
            data.put("itemid", "" + assetid);
            fetch(baseTradeURL + "removeitem", "POST", data);
        }

        public boolean setReady(boolean ready) {
            final Map<String, String> data = new HashMap<>();
            try {
                data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            data.put("ready", ready ? "true" : "false");
            data.put("version", "" + version);
            final String response = fetch(baseTradeURL + "toggleready", "POST", data);
            try {
                Status readyStatus = new Status((JSONObject) new JSONParser().parse(response));
                if (readyStatus.success) {
                    if (readyStatus.trade_status == 0) {
                        otherReady = readyStatus.them.ready;
                        meReady = readyStatus.me.ready;
                    } else {
                        meReady = true;
                    }
                    return meReady;
                }
            } catch (final ParseException e) {
                e.printStackTrace();
            }
            return false;
        }

        public JSONObject acceptTrade() throws ParseException {
            final Map<String, String> data = new HashMap<>();
            try {
                data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            data.put("version", "" + version);
            final String response = fetch(baseTradeURL + "confirm", "POST", data);

            return (JSONObject) new JSONParser().parse(response);
        }

        /**
         * Cancels the trade session as if we clicked the "Cancel Trade" button.
         *
         * @return True if server responded as successful, false otherwise.
         * @throws ParseException when there is an error in parsing the
         * response.
         */
        public boolean cancelTrade() throws ParseException {
            final Map<String, String> data = new HashMap();
            try {
                data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            final String response = fetch(baseTradeURL + "cancel", "POST", data);

            if (response == null) {
                return false;
            }
            return (boolean) ((JSONObject) new JSONParser().parse(response)).get("success");
        }

        public String sendMessage(String message) {
            final Map<String, String> data = new HashMap<>();
            try {
                data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            data.put("message", message);

            return fetch(baseTradeURL + "chat", "POST", data);
        }

        protected String fetch(String url, String method, Map<String, String> data) {
            return fetch(url, method, data, true);
        }

        protected String fetch(String url, String method, Map<String, String> data, boolean sendLoginData) {
            String cookies = "";
            if (sendLoginData) {
                try {
                    cookies = "sessionid=" + URLEncoder.encode(sessionId, "UTF-8") + "; steamLogin=" + steamLogin + ";";
                } catch (UnsupportedEncodingException e) {
                }
            }
            final String response = request(url, method, data, cookies);
            return response;
        }

        String request(String url, String method, Map<String, String> data, String cookies) {
            boolean ajax = true;
            StringBuilder out = new StringBuilder();
            try {
                String dataString = "";
                if (data != null) {
                    for (final String key : data.keySet()) {
                        dataString += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(data.get(key), "UTF-8") + "&";
                    }
                }
                if (!method.equals("POST")) {
                    url += "?" + dataString;
                }
                final URL url2 = new URL(url);
                final HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
                conn.setRequestProperty("Cookie", cookies);
                conn.setRequestMethod(method);
                System.setProperty("http.agent", "");
                /**
                 * Previous User-Agent String for reference:
                 * "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; Valve Steam
                 * Client/1392853084; SteamTrade-Java Client; )
                 * AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166
                 * Safari/535.19"
                 */
                conn.setRequestProperty("User-Agent", "SteamTrade-Java/1.0 (Windows; U; Windows NT 6.1; en-US; Valve Steam Client/1392853084; SteamTrade-Java Client; ) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Safari/535.19");
                conn.setRequestProperty("Host", "steamcommunity.com");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("Accept", "text/javascript, text/hml, application/xml, text/xml, */*");

                // I don't know why, but we need a referer, otherwise we get a server error response.
                // Just use our trade URL as the referer since we have it on hand.
                conn.setRequestProperty("Referer", baseTradeURL);

                // Accept compressed responses.  (We can decompress it.)
                conn.setRequestProperty("Accept-Encoding", "gzip,deflate");

                if (ajax) {
                    conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                    conn.setRequestProperty("X-Prototype-Version", "1.7");
                }

                if (method.equals("POST")) {
                    conn.setDoOutput(true);
                    final OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
                    os.write(dataString.substring(0, dataString.length() - 1));
                    os.flush();
                }

                java.io.InputStream netStream = conn.getInputStream();

                // If GZIPped response, then use the gzip decoder.
                if (conn.getContentEncoding().contains("gzip")) {
                    netStream = new java.util.zip.GZIPInputStream(netStream);
                }

                //cookies = conn.getHeaderField("Set-Cookie");
                final BufferedReader reader = new BufferedReader(new InputStreamReader(netStream));

                String line; // Stores an individual line currently being read.
                while ((line = reader.readLine()) != null) {
                    if (out.length() > 0) {
                        out.append('\n');
                    }
                    out.append(line);
                }
            } catch (final MalformedURLException e) {
                e.printStackTrace();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            return out.toString();
        }
    }
}

/**
 * Brutally scrapes the AppContextData JavaScript object from the trade page.
 * Without this, we would not know what inventories we have.
 *
 * @author nosoop
 */
class ContextScraper {

    private static final List<AppContextPair> DEFAULT_APPCONTEXTDATA = new ArrayList<>();

    /**
     * Initialize default AppContextPairs.
     */
    static {
        DEFAULT_APPCONTEXTDATA.add(new AppContextPair(440, 2, "Team Fortress 2"));
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
    static List<AppContextPair> scrapeContextData(String pageResult) {
        try {
            BufferedReader read;
            read = new BufferedReader(new StringReader(pageResult));

            String buffer;
            while ((buffer = read.readLine()) != null) {
                String input;
                input = buffer.trim();

                if (input.startsWith("var g_rgAppContextData")) {
                    // Extract the JSON string from the JavaScript source.  Bleh
                    input = input.substring(input.indexOf('{'), input.length() - 1);
                    return parseContextData(input);
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
    private static List<AppContextPair> parseContextData(String json) {
        List<AppContextPair> result = new ArrayList<>();

        try {
            JSONObject feedData = (JSONObject) (new JSONParser()).parse(json);

            for (JSONObject o : (Collection<JSONObject>) feedData.values()) {
                String gameName = (String) o.get("name");
                int appid = (int) (long) o.get("appid");

                JSONObject contextData = (JSONObject) o.get("rgContexts");

                for (JSONObject b : (Collection<JSONObject>) contextData.values()) {
                    String contextName = (String) b.get("name");
                    long contextid = Long.parseLong((String) b.get("id"));
                    int assetCount = (int) (long) b.get("asset_count");

                    // "Team Fortress 2 - Backpack (226)"
                    String invNameFormat = String.format("%s - %s (%d)", gameName, contextName, assetCount);

                    // Only include the inventory if it's not empty.
                    if (assetCount > 0) {
                        result.add(new AppContextPair(appid, contextid, invNameFormat));
                    }
                }
            }
            return result;
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return result;
    }
}
