/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poonso.scrapbanktf.status;

import org.json.simple.JSONObject;

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

    public TradeEvent(JSONObject event) {
        jsonObject = event;

        steamid = (String) event.get("steamid");
        action = Integer.parseInt((String) event.get("action"));
        timestamp = (long) event.get("timestamp");
        appid = (int) (long) event.get("appid");
        text = (String) event.get("text");

        // contextid required for private inventory only.
        if (event.containsKey("contextid")) {
            contextid = Long.valueOf((String) event.get("contextid"));
        }

        // assetid required for getting item info from public inventory.
        if (event.containsKey("assetid")) {
            assetid = Long.valueOf((String) event.get("assetid"));
        }

        // amount required when dealing in currency
        if (event.containsKey("amount")) {
            amount = Integer.parseInt((String) event.get("amount"));
        }
    }

    public JSONObject getJSONObject() {
        return jsonObject;
    }
}