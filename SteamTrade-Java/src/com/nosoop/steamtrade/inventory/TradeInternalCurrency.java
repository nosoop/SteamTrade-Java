/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author nosoop
 */
public class TradeInternalCurrency extends TradeInternalItem {

    int amount;
    
    TradeInternalCurrency(long id, JSONObject rgDescriptionItem) throws JSONException {
        super(id, rgDescriptionItem);
    }
    
    public int getAmount() {
        return amount;
    }
    // TODO Add method to return overridable name instead of using basic display.
}
