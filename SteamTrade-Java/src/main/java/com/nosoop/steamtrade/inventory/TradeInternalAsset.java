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
     * The inventory this item resides in, as defined by its appid and contextid
     * pair.
     */
    AppContextPair appContext;
    /**
     * The display name of the item. If the item was renamed (as it could be in
     * TF2, it will be that name.
     */
    String name;
    /**
     * The name it would be grouped under in the Steam Community Market. Is
     * an empty string if not in the Market.
     */
    String marketName;
    /**
     * The item's type.
     */
    String type;
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
     * The item id number.
     */
    long assetid;

    /**
     * Creates a new TradeInternalAsset instance.
     *
     * @param rgInventoryItem
     * @param rgDescriptionItem
     * @throws JSONException
     */
    TradeInternalAsset(AppContextPair appContext, JSONObject rgInventoryItem,
            JSONObject rgDescriptionItem) throws JSONException {
        String classidString;
        this.appContext = appContext;

        this.name = rgDescriptionItem.getString("name");
        this.marketName = rgDescriptionItem.getString("market_name");
        this.type = rgDescriptionItem.getString("type");

        classidString = rgInventoryItem.getString("classid");
        this.classid = Integer.parseInt(classidString);

        this.amount = Integer.parseInt(
                rgInventoryItem.optString("amount", "1"));
        this.assetid = Long.parseLong(rgInventoryItem.getString("id"));;

        /**
         * Verify that the input appid is the same appid passed in the
         * constructor.
         */
        int descriptionAppid = Integer.parseInt(
                rgDescriptionItem.getString("appid"));
        assert (descriptionAppid == this.appContext.appid);

        /**
         * Assert that the classid matches rgDescription and rgCurrency or
         * rgInventory by hash.
         */
        String descriptionClassidString = 
                rgDescriptionItem.getString("classid");
        assert (descriptionClassidString.equals(classidString));
    }

    /**
     * Returns the display name of this asset, defaulting to its name. Allowed
     * to be overridden by subclasses.
     *
     * @return String representing the name of this asset.
     */
    public String getDisplayName() {
        return getName();
    }

    /**
     * Returns the name of this asset.
     *
     * @return The name of this asset, as defined by the "name"-named name-value
     * pair in the item's "rgDescriptions" JSONObject member entry.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the market name of this asset.
     *
     * @return The market name of this asset. If it does not have a market name,
     * it returns an empty string.
     */
    public final String getMarketName() {
        return marketName;
    }

    /**
     * Returns the appid of this asset.
     *
     * @return The appid of this asset. Defined by the AppContextPair instance
     * passed to it, this is asserted to be the same value as the one defined in
     * the "appid"-named name-value pair in the item's "rgDescriptions"
     * JSONObject member entry.
     */
    public final int getAppid() {
        return appContext.appid;
    }

    /**
     * Returns the contextid of this asset.
     *
     * @return The contextid of this asset. Defined by the AppContextPair
     * instance passed to it, unlike the appid, there is no way to verify that
     * the contextid is correctly defined.
     */
    public final long getContextid() {
        return appContext.contextid;
    }

    /**
     * Returns the assetid of this asset.
     *
     * @return The assetid of this asset, defined by the "id"-named name-value
     * pair of this asset's "rgInventory" JSONObject member entry.
     */
    public final long getAssetid() {
        return assetid;
    }

    /**
     * Returns the classid of this asset.
     *
     * @return The classid of this asset, defined by the "classid"-named
     * name-value pair of the asset's "rgDescription" JSONObject member entry
     * and asserted to be equal to the similar value in the asset's
     * "rgInventory" entry.
     */
    public final int getClassid() {
        return classid;
    }

    /**
     * Returns the amount of this asset.
     *
     * @return The amount of this asset, defined by the "amount"-named
     * name-value pair of the asset's "rgInventory" JSONObject member entry
     */
    public final int getAmount() {
        return amount;
    }

    /**
     * Returns the type of this asset.
     *
     * @return The amount of this asset, defined by the "type"-named
     * name-value pair of the asset's "rgDescription" JSONObject member entry
     */
    public final String getType() {
        return type;
    }
}
