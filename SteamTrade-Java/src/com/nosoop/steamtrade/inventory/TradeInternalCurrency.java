/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 *
 * @author nosoop
 */
public class TradeInternalCurrency extends TradeInternalItem {
    
    int amount;
    long currencyid;
    
    TradeInternalCurrency(long id, JSONObject rgDescriptionItem) throws JSONException {
        super(-1, rgDescriptionItem);
        
        currencyid = id;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public long getCurrencyId() {
        return currencyid;
    }
    // TODO Add method to return overridable name instead of using basic display.
}
