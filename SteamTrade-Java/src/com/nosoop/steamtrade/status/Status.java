package com.nosoop.steamtrade.status;

import com.nosoop.steamtrade.TradeListener;
import com.nosoop.steamtrade.TradeListener.TradeStatusCodes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
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
    public int trade_status = -1;
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
            trade_status = obj.getInt("trade_status");

            if (trade_status == 0) {
                newversion = obj.getBoolean("newversion");
                version = obj.getInt("version");
                if (obj.has("logpos")) {
                    logpos = obj.getInt("logpos");
                }

                me = new TradeUserStatus(obj.getJSONObject("me"));
                them = new TradeUserStatus(obj.getJSONObject("them"));

                JSONArray statusEvents = obj.optJSONArray("events");

                if (statusEvents != null) {
                    for (int i = 0; i < statusEvents.length(); i++) {
                        events.add(new TradeEvent(statusEvents.getJSONObject(i)));
                    }
                }
            }
        } else {
            error = obj.optString("error", "(No error message.)");

            /**
             * If there is an error we should know about that isn't defined, we
             * should add a custom status code to pick it up.
             */
            trade_status = obj.optInt("trade_status",
                    TradeStatusCodes.STATUS_ERRORMESSAGE);
        }
    }
}