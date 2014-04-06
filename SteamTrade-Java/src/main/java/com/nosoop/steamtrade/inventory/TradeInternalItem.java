package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONArray;
import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * Class representation of an item in a user's inventory.
 *
 * @author nosoop
 */
public class TradeInternalItem {

    public static final TradeInternalItem UNAVAILABLE = null;
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
    long assetid;
    int defIndex;
    byte level;
    byte quality;
    boolean isNotCraftable;
    int appid;
    long contextid;
    boolean isTradable;
    boolean wasGifted;
    int classid;
    long instanceid;
    // TODO Implementation of stackable items.
    boolean stackable;

    TradeInternalItem(JSONObject rgInventoryItem, JSONObject rgDescriptionItem) throws JSONException {

        this.displayName = rgDescriptionItem.getString("name");
        this.marketName = rgDescriptionItem.getString("market_name");

        // Unique item identifiers:
        // --  classid represents similarly unique items (ex: two Loose Cannons)
        // --  instanceid represents unique items in that class ("0" = default)
        this.classid = Integer.parseInt(rgDescriptionItem.getString("classid"));
        this.instanceid =
                Long.parseLong(rgDescriptionItem.optString("instanceid", "0"));
        // Currency items do not have instance numbers.

        this.wasGifted = false;

        this.assetid = Long.parseLong(rgInventoryItem.getString("id"));;
        level = (byte) -1;

        isNotCraftable = false;

        // Iterate through descriptions.
        JSONArray descs = rgDescriptionItem.optJSONArray("descriptions");
        if (descs != null) {
            for (int i = 0; i < descs.length(); i++) {
                JSONObject descriptionItem = descs.getJSONObject(i);
                String descriptionValue = descriptionItem.getString("value");

                // TODO Make this language independent?
                if (descriptionValue.contains("Gift from")) {
                    wasGifted = true;
                }
            }
        }

        // Assume non-tradable if it does not have a value for "tradable".
        isTradable = rgDescriptionItem.optInt("tradable", 0) == 1;

        // TF2-specific stuff.
        JSONObject appData = rgDescriptionItem.optJSONObject("app_data");
        if (appData != null) {
            if (appData.has("def_index")) {
                defIndex = Integer.parseInt(appData.getString("def_index"));
            }

            if (appData.has("quality")) {
                quality = (byte) Integer.parseInt(appData.getString("quality"));
            }
        }
    }
    // TODO Add method to return overridable name instead of using basic display?

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

    public long getInstanceid() {
        return instanceid;
    }

    public int getClassid() {
        return classid;
    }

    /**
     * Gets the definition index for the item.
     *
     * @return The item's definition index.
     * @deprecated It's only available for TF2, just about. TODO: Add support
     * for subclassing TradeInternalItem?
     */
    @Deprecated
    public int getDefIndex() {
        return defIndex;
    }

    public boolean isRenamed() {
        return !marketName.equals(displayName) && displayName.matches("''.*''");
    }

    public boolean wasGifted() {
        return wasGifted;
    }
}