package com.nosoop.steamtrade.inventory;

import java.util.ArrayList;
import java.util.List;
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
    public long assetid;
    public int defIndex;
    public byte level;
    public byte quality;
    public boolean isNotCraftable;
    public List<ItemAttribute> attributes = new ArrayList<>();
    public int appid;
    public long contextid;
    boolean isTradable;
    public boolean wasGifted;
    // TODO Implementation of stackable items.
    boolean stackable;
    
    TradeInternalItem(long assetid, JSONObject rgDescriptionItem) throws JSONException {

        this.marketName = rgDescriptionItem.getString("market_name");
        this.displayName = rgDescriptionItem.getString("name");

        this.wasGifted = false;

        this.assetid = assetid;
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

        isRenamed = (!marketName.equals(displayName) && displayName.matches("''.*''"));

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
}