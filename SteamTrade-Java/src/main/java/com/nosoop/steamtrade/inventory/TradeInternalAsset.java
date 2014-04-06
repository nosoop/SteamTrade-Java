/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * Represents a generic, tradable asset and its basic characteristics.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public abstract class TradeInternalAsset {
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
     * The Steam appid for the game this object resides in.
     */
    int appid;
    /**
     * The inventory contextid for the game inventory this object resides in.
     */
    long contextid;
    /**
     * The item id number.
     */
    long assetid;

    /**
     * Creates a new TradeInternalObject instance.
     *
     * @param rgInventoryItem
     * @param rgDescriptionItem
     * @throws JSONException
     */
    TradeInternalAsset(JSONObject rgInventoryItem,
            JSONObject rgDescriptionItem) throws JSONException {
        this.displayName = rgDescriptionItem.getString("name");
        this.marketName = rgDescriptionItem.getString("market_name");
        this.classid = Integer.parseInt(rgDescriptionItem.getString("classid"));

        this.amount = Integer.parseInt(
                rgInventoryItem.optString("amount", "-1"));
        this.assetid = Long.parseLong(rgInventoryItem.getString("id"));;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMarketName() {
        return marketName;
    }

    public int getAppid() {
        return appid;
    }

    public long getContextid() {
        return contextid;
    }

    public long getAssetid() {
        return assetid;
    }

    public int getClassid() {
        return classid;
    }
}
