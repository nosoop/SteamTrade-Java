/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.status;

import org.json.simple.JSONObject;

/**
 *
 *
 * @author nosoop
 */
public class TradeAssetsObj {
    
    int appid;
    long contextid;
    long assetid;
    
    long amount;
    
    public TradeAssetsObj(JSONObject obj) {
        this.appid = Integer.parseInt((String) obj.get("appid"));
        this.contextid = Long.parseLong((String) obj.get("contextid"));
        this.assetid = Long.parseLong((String) obj.get("assetid"));
        
        if (obj.containsKey("amount")) {
            this.amount = Long.parseLong((String) obj.get("amount"));
        }
    }
    
}
