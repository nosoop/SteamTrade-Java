package com.poonso.scrapbanktf.status;

import com.poonso.scrapbanktf.trade.TradeAssetsObj;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents user status in trade.
 * 
 * @author nosoop
 */
public class TradeUserStatus {

    public boolean ready;
    public boolean confirmed;
    public int sec_since_touch;
    public List<TradeAssetsObj> assets;

    public TradeUserStatus(JSONObject obj) {
        ready = (long) obj.get("ready") == 1;
        confirmed = (long) obj.get("confirmed") == 1;
        sec_since_touch = (int) (long) obj.get("sec_since_touch");

        // TODO Add asset support to update variable-item quantities.
        Object assetsRef = obj.get("assets");

        if (assetsRef instanceof JSONArray) {
            assets = new ArrayList<>();

            for (Object asset : ((JSONArray) assetsRef).toArray()) {
                assets.add(new TradeAssetsObj((JSONObject) asset));
            }
        }
    }
}