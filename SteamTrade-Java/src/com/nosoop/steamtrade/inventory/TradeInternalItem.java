package com.nosoop.steamtrade.inventory;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class representation of an item in a user's inventory.
 * 
 * @author nosoop
 */
public class TradeInternalItem {

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
    //public int position;
    public boolean isNotCraftable;
    public List<ItemAttribute> attributes = new ArrayList<>();
    public int appid;
    public long contextid;
    boolean isTradable;
    public boolean wasGifted;
    // TODO Implementation of stackable items.
    boolean stackable;

    TradeInternalItem(long assetid, JSONObject rgDescriptionItem) throws JSONException {
        JSONObject appData = rgDescriptionItem.optJSONObject("app_data");

        this.marketName = rgDescriptionItem.getString("market_name");
        this.displayName = rgDescriptionItem.getString("name");

        this.wasGifted = false;

        this.assetid = assetid;
        level = (byte) -1;

        if (appData != null) {
            if (appData.has("def_index")) {
                defIndex = Integer.parseInt(appData.getString("def_index"));
            }

            if (appData.has("quality")) {
                quality = (byte) Integer.parseInt(appData.getString("quality"));
            }
        }

        isNotCraftable = false;
        final Object attrs = null; //obj.get("attributes");
        if (attrs != null && attrs instanceof ArrayList<?>) {
            for (final JSONObject attr : (ArrayList<JSONObject>) attrs) {
                attributes.add(new ItemAttribute(attr));
            }
        }

        // Iterate through descriptions.
        final Object descs = rgDescriptionItem.get("descriptions");
        if (descs != null && descs instanceof ArrayList<?>) {
            for (final JSONObject descriptionItem : (ArrayList<JSONObject>) descs) {
                // TODO Make this language independent?
                String descriptionValue = descriptionItem.getString("value");
                
                if (descriptionValue.contains("Gift from")) {
                    wasGifted = true;
                }
            }
        }

        isRenamed = (!marketName.equals(displayName) && displayName.matches("''.*''"));

        isTradable = rgDescriptionItem.has("tradable")
                ? rgDescriptionItem.getInt("tradable") == 1 : false;
    }
    // TODO Add method to return overridable name instead of using basic display?
}