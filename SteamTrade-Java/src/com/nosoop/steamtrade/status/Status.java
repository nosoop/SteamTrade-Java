package com.nosoop.steamtrade.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONException;

import org.json.JSONObject;

/**
 * Object representing current trade state. (No modifications.)
 *
 * @author Top-Cat
 */
public class Status {

    public String error;
    public boolean newversion;
    public boolean success;
    public long trade_status = -1;
    public int version;
    public int logpos;
    public TradeUserStatus me;
    public TradeUserStatus them;
    public List<TradeEvent> events = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public Status(JSONObject obj) throws JSONException {
        success = obj.getBoolean("success");
        
        if (success) {
            error = "None";
            trade_status = obj.getLong("trade_status");
            
            if (trade_status == 0) {
                newversion = obj.getBoolean("newversion");
                version = obj.getInt("version");
                if (obj.has("logpos")) {
                    logpos = obj.getInt("logpos");
                }
                
                me = new TradeUserStatus(obj.getJSONObject("me"));
                them = new TradeUserStatus(obj.getJSONObject("them"));
                
                JSONObject statusEvents = obj.optJSONObject("events");
                if (statusEvents != null) {
                    for (final String event : (Set<String>) statusEvents.keySet()) {
                        events.add(new TradeEvent(obj.getJSONObject("events").getJSONObject(event)));
                    }
                }
            }
        } else {
            error = obj.getString("error");
        }
    }
}