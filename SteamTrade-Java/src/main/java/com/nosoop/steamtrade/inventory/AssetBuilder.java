/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * Extendable class that builds TradeInternalItem and TradeInternalCurrency
 * instances.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class AssetBuilder {
    public TradeInternalItem generateItem(AppContextPair appContext,
            JSONObject rgInventory, JSONObject rgDescription)
            throws JSONException {
        return new TradeInternalItem(appContext, rgInventory, rgDescription);
    }

    public TradeInternalCurrency generateCurrency(AppContextPair appContext,
            JSONObject rgInventory, JSONObject rgDescription)
            throws JSONException {
        return new TradeInternalCurrency(appContext, rgInventory, rgDescription);
    }
}
