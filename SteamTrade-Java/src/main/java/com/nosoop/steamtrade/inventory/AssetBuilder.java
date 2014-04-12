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
public abstract class AssetBuilder {
    /**
     * Determines whether or not this AssetBuilder instance should handle
     * loading a given inventory. For subclassed AssetBuilders, override this
     * method.
     *
     * @param appContext An appid-contextid pair for an inventory.
     * @return Whether or not this AssetBuilder instance should handle the
     * inventory.
     */
    public abstract boolean isSupported(AppContextPair appContext);

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
