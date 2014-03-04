/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.status;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * @author nosoop
 */
public class TradeEvent {

    public String steamid;
    public int action;
    public long timestamp;
    public int appid;
    public String text;
    public long contextid;
    public long assetid;
    // Currency-based.
    public int amount;
    public long currencyid;
    JSONObject jsonObject;

    /* Reference to trade action ID's
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
    public class TradeAction {

        public static final int ITEM_ADDED = 0,
                ITEM_REMOVED = 1,
                READY_TOGGLED = 2,
                READY_UNTOGGLED = 3,
                // 4, 5
                CURRENCY_CHANGED = 6,
                MESSAGE_ADDED = 7,
                STACKABLE_CHANGED = 8;
    }

    TradeEvent(JSONObject event) throws JSONException {
        jsonObject = event;

        steamid = event.getString("steamid");
        action = Integer.parseInt(event.getString("action"));
        timestamp = event.getLong("timestamp");
        appid = event.getInt("appid");
        text = event.optString("text");

        // contextid required for private inventory only.
        if (event.has("contextid")) {
            contextid = Long.valueOf(event.getString("contextid"));
        }

        // assetid required for getting item info from public inventory.
        if (event.has("assetid")) {
            assetid = Long.valueOf(event.getString("assetid"));
        }

        // amount required when dealing in currency
        if (event.has("amount")) {
            amount = Integer.parseInt(event.getString("amount"));
        }
        
        if (event.has("currencyid")) {
            currencyid = Long.parseLong(event.getString("currencyid"));
        }
    }

    public JSONObject getJSONObject() {
        return jsonObject;
    }
}