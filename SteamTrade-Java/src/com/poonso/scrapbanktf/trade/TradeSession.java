package com.poonso.scrapbanktf.trade;

import com.poonso.scrapbanktf.status.TradeEvent;
import com.poonso.scrapbanktf.status.Status;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.poonso.scrapbanktf.inventory.AppContextPair;
import com.poonso.scrapbanktf.inventory.TradeInternalInventories;
import com.poonso.scrapbanktf.inventory.TradeInternalItem;
import com.poonso.scrapbanktf.trade.TradeListener.TradeErrorCodes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object representing a session of a trade. (Changes: Renamed to TradeSession,
 * added support to cancel the trade, added onWelcome() support to notify the
 * listener that a trade is being opened, added support to load the other
 * person's inventory without ever touching the WebAPI, removed support of
 * loading inventories with the WebAPI, added ability to load any of the other
 * player's inventories.)
 *
 * @author Top-Cat, nosoop
 */
public class TradeSession implements Runnable {

    // Static properties
    public static String SteamCommunityDomain = "steamcommunity.com";
    public static String SteamTradeUrl = "http://steamcommunity.com/trade/%s/";
    // Generic Trade info
    public boolean meReady = false, otherReady = false;
    boolean tradeStarted = false;
    int lastEvent = 0;
    public String pollLock2 = "";
    //
    // The items put up for offer.
    // TODO Replace with TradeInternalItems, or make it an array of?
    public Set<Long> MyTrade = new HashSet<>();
    public Set<Long> OtherTrade = new HashSet<>();
    public Object[] trades;
    //
    // The inventories of both users.
    public TradeInternalInventories otherUserTradeInventories;
    public TradeInternalInventories myTradeInventories;
    // 
    public List<AppContextPair> myAppContextData;
    //
    // Trade interfacing object.
    protected TradeWebAPI tradeAPI;
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
    public TradeSession(long steamidSelf, long steamidPartner, String sessionId, String token, TradeListener listener) throws Exception {
        steamIdSelf = steamidSelf;
        steamIdPartner = steamidPartner;

        trades = new Object[]{MyTrade, OtherTrade};

        this.sessionId = sessionId;
        steamLogin = token;

        listener.trade = this;
        tradeListener = listener;

        baseTradeURL = String.format(TradeSession.SteamTradeUrl, steamIdPartner);

        tradeAPI = new TradeWebAPI(baseTradeURL, this.sessionId, steamLogin);

        myTradeInventories = new TradeInternalInventories();
        otherUserTradeInventories = new TradeInternalInventories();

        try {
            tradeListener.onWelcome();

            scrapeBackpackContexts();

            tradeListener.onAfterInit();
        } catch (final Exception e) {
            tradeListener.onError(TradeErrorCodes.INITIALIZATION_ERROR);
            e.printStackTrace();
            throw e;
        }

    }
    public Status status = null;

    /**
     * Polls the TradeSession for updates.
     */
    @SuppressWarnings("unchecked")
    public void run() {
        synchronized (pollLock2) {
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
                tradeListener.onError(TradeErrorCodes.TRADE_CANCELLED);
            } else if (status.trade_status == 4) {
                // Other user timed out according to trade system.
                tradeListener.onError(TradeErrorCodes.PARTNER_TIMED_OUT);
            } else if (status.trade_status == 5) {
                // Trade failed.
                tradeListener.onError(TradeErrorCodes.TRADE_FAILED);
            } else if (status.trade_status == 1) {
                // Trade successful.
                tradeListener.onComplete();
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

        /* Trade Action ID's
         * 0 = Add item (itemid = "assetid")
         * 1 = Remove item (itemid = "assetid")
         * 2 = Toggle ready
         * 3 = Toggle not ready
         * 4 = ?
         * 5 = ? - maybe some sort of cancel
         * 6 = Add / remove currency.
         *     (SK Crowns/CE fall into this, but other SK items do 
         *     not.)
         * 7 = Chat (message = "text")
         * 8 = Updated variable count item?
         *     (Other SK items fall into this, but on initial add it
         *     uses action 0.)
         */
        switch (evt.action) {
            case 0:
                eventUserAddedItem(evt);
                break;
            case 1:
                eventUserRemovedItem(evt);
                break;
            case 2:
                if (!isBot) {
                    otherReady = true;
                    tradeListener.onUserSetReadyState(true);
                } else {
                    meReady = true;
                }
                break;
            case 3:
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
            case 7:
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

        ((Set<Long>) trades[isBot ? 0 : 1]).add(evt.assetid);
        if (!isBot) {
            if (!otherUserTradeInventories.hasInventory(evt.appid, evt.contextid)) {
                addForeignInventory(steamIdPartner, evt.appid, evt.contextid);
            }
            final TradeInternalItem item = otherUserTradeInventories.getInventory(evt.appid, evt.contextid).getItem(evt.assetid);
            tradeListener.onUserAddItem(item);
        }
    }

    private void eventUserRemovedItem(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(String.valueOf(steamIdPartner));
        ((Set<Long>) trades[isBot ? 0 : 1]).remove(evt.assetid);
        if (!isBot) {
            final TradeInternalItem item = otherUserTradeInventories.getInventory(evt.appid, evt.contextid).getItem(evt.assetid);
            tradeListener.onUserRemoveItem(item);
        }
    }

    private void eventUserSetCurrencyAmount(TradeEvent evt) {
        boolean isBot = !evt.steamid.equals(String.valueOf(steamIdPartner));
        // TODO Set support for currency?
        if (!isBot) {
        }
    }

    /**
     * Loads a copy of the trade screen, passing the data to ContextScraper to
     * generate a list of AppContextPairs as reference to load inventories with.
     */
    private void scrapeBackpackContexts() {
        // I guess we're scraping the trade page.
        final Map<String, String> data = new HashMap<>();

        String pageData = fetch(baseTradeURL, "GET", data);
        List<AppContextPair> contexts = ContextScraper.scrapeContextData(pageData);

        myAppContextData = contexts;
    }

    /**
     * Submits a message into the current trade chat.
     *
     * @param msg Message to be sent.
     * @return Server response to the chat message.
     */
    public final String sendMessage(String msg) {
        final Map<String, String> data = new HashMap<>();
        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("message", msg);
        data.put("logpos", "" + logpos);
        data.put("version", "" + version);
        return fetch(baseTradeURL + "chat", "POST", data);
    }

    public void addItem(TradeInternalItem item, int slot) {
        tradeAPI.addItem(item.appid, item.contextid, item.assetid, slot);
    }

    public void addItem(long itemid, int slot) {
        tradeAPI.addItem(440, 2, itemid, slot);
    }

    public void removeItem(TradeInternalItem item) {
        tradeAPI.removeItem(item.appid, item.contextid, item.assetid);
    }

    public void removeItem(long itemid) {
        tradeAPI.removeItem(440, 2, itemid);
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
     * @return Boolean value true if server responded as successful, false
     * otherwise.
     * @throws ParseException when there is an error in parsing the response.
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

    // TODO Reduce polling rate?
    protected Status getStatus() throws ParseException {
        final Map<String, String> data = new HashMap<>();
        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("logpos", "" + logpos);
        data.put("version", "" + version);

        final String response = fetch(baseTradeURL + "tradestatus/", "POST", data);

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

        response = fetch(url, "GET", null, true);

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

        String feed = fetch(baseTradeURL + "foreigninventory", "POST", data);

        otherUserTradeInventories.addInventory(appId, contextId, feed);
    }

    protected String fetch(String url, String method, Map<String, String> data) {
        return fetch(url, method, data, true);
    }

    // TODO Make the web thingy a pluggable system?
    protected String fetch(String url, String method, Map<String, String> data, boolean sendLoginData) {
        String cookies = "";
        if (sendLoginData) {
            cookies = "sessionid=" + sessionId + "; steamLogin=" + steamLogin;
        }
        final String response = tradeAPI.request(url, method, data, cookies);
        return response;
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
