/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * Represents a tradable item and characteristics of them that can be applied
 * across all items..
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public abstract class TradeInternalObject {
    /**
     * The display name of the item. If the item was renamed (as it could be in
     * TF2, it will be that name.
     */
    String displayName;
    /**
     * The name it would be grouped under in the Steam Community Market. Is
     * blank if not in the Market.
     */
    String marketName;
    /**
     * The class number of this object. Two similar items (e.g., a pair of Loose
     * Cannons) will have the same class number.
     */
    int classid;
    /**
     * The number of items of this object has.
     */
    int amount;

    /**
     * Creates a new TradeInternalObject instance.
     *
     * @param rgInventoryItem
     * @param rgDescriptionItem
     * @throws JSONException
     */
    TradeInternalObject(JSONObject rgInventoryItem,
            JSONObject rgDescriptionItem) throws JSONException {

        this.displayName = rgDescriptionItem.getString("name");
        this.marketName = rgDescriptionItem.getString("market_name");
        this.classid = Integer.parseInt(rgDescriptionItem.getString("classid"));

        this.amount = Integer.parseInt(rgInventoryItem.getString("amount"));
    }
}
