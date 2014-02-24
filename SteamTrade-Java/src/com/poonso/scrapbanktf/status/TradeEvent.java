/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poonso.scrapbanktf.status;

import java.util.EnumMap;
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
    
    public enum Type {
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
        EItemAdded, EItemRemoved, EReady, EUnready,
        ECurrencyModified, EChatMessage, EItemUpdated, EUndefined;
    }
    
    private static final EnumMap<TradeEvent.Type,Integer> types;
    static {
        types = new EnumMap<>(TradeEvent.Type.class);
        
        types.put(Type.EItemAdded, 0);
        types.put(Type.EItemRemoved, 1);
        types.put(Type.EReady, 2);
        types.put(Type.EUnready, 3);
        types.put(Type.ECurrencyModified, 6);
        types.put(Type.EChatMessage, 7);
        types.put(Type.EItemUpdated, 8);
    }
}