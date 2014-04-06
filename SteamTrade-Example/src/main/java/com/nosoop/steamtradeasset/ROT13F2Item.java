/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtradeasset;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

/**
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class ROT13F2Item extends TradeInternalItem {
    public ROT13F2Item(AppContextPair appContext, JSONObject rgInventoryItem, JSONObject rgDescriptionItem) throws JSONException {
        super(appContext, rgInventoryItem, rgDescriptionItem);

        // extend with item info
    }
}
