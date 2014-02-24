package com.nosoop.steamtrade.status;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

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
    public Status(JSONObject obj) {
        success = (boolean) obj.get("success");
        if (success) {
            error = "None";
            trade_status = (long) obj.get("trade_status");

            if (trade_status == 0) {
                newversion = (boolean) obj.get("newversion");
                version = (int) (long) obj.get("version");
                if (obj.containsKey("logpos")) {
                    logpos = (int) obj.get("logpos");
                }
                
                me = new TradeUserStatus((JSONObject) obj.get("me"));
                them = new TradeUserStatus((JSONObject) obj.get("them"));
                
                if (obj.get("events") != null) {
                    for (final JSONObject event : (ArrayList<JSONObject>) obj.get("events")) {
                        events.add(new TradeEvent(event));
                    }
                }
            }
        } else {
            error = (String) obj.get("error");
        }
    }
}