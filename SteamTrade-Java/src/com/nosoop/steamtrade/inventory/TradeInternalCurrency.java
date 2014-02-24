/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import org.json.simple.JSONObject;

/**
 *
 * @author nosoop
 */
public class TradeInternalCurrency {

    public boolean isRenamed;
    
    /**
     * The display name of the item. If the item was renamed (as it could be in
     * TF2, it will be that name.
     */
    public String displayName;
    
    /**
     * The name it would be grouped under in the Steam Community Market. Is
     * blank if not in the Market.
     */
    public String marketName;
    public long id;
    public int appid;
    public long contextid;

    TradeInternalCurrency(long id, JSONObject rgDescriptionItem) {

        marketName = (String) rgDescriptionItem.get("market_name");
        displayName = (String) rgDescriptionItem.get("name");
        
        System.out.println("TradeInternalCurrency: Added " + displayName);

        this.id = id;

    }
    // TODO Add method to return overridable name instead of using basic display.
}
