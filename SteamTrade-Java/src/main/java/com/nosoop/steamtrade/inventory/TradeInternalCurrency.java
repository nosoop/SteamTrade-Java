/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * Class representation of a currency item in a user's inventory.
 * 
 * @author nosoop
 */
public class TradeInternalCurrency extends TradeInternalItem {
    
    long currencyid;
    
    public TradeInternalCurrency(JSONObject rgCurrencyItem,
            JSONObject rgDescriptionItem) throws JSONException {
        super(rgCurrencyItem, rgDescriptionItem);
        
        currencyid = Integer.parseInt(rgCurrencyItem.getString("id"));
        amount = Integer.parseInt(rgCurrencyItem.getString("amount"));
    }
    
    public int getAmount() {
        return amount;
    }
    
    public long getCurrencyId() {
        return currencyid;
    }
    // TODO Add method to return overridable name instead of using basic display.
}
