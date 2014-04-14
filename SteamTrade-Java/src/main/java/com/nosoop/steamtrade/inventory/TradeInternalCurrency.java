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
public class TradeInternalCurrency extends TradeInternalAsset {
    
    long currencyid;
    
    public TradeInternalCurrency(AppContextPair appContext,
            JSONObject rgCurrencyItem, JSONObject rgDescriptionItem)
            throws JSONException {
        super(appContext, rgCurrencyItem, rgDescriptionItem);
        
        currencyid = Long.parseLong(rgCurrencyItem.getString("id"));
    }
    
    public long getCurrencyId() {
        return currencyid;
    }
}
