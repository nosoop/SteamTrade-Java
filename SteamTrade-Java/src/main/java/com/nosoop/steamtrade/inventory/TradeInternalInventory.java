package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONObject;
import bundled.steamtrade.org.json.JSONException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Steam user's inventory as displayed in trading and from viewing
 * an inventory online.
 *
 * @author nosoop (ported and adapted from ForeignInventory in the SteamBot
 * project by Jessecar96)
 */
public class TradeInternalInventory {
    boolean inventoryValid;
    String errorMessage;
    List<TradeInternalItem> inventoryItems;
    // Debating on implementation for currency items.
    List<TradeInternalCurrency> currencyItems;
    final AppContextPair appContext;
    final AssetBuilder assetBuilder;

    /**
     * Takes a String representation of the JSON data received from trading and
     * creates a representation of the inventory.
     *
     * @param s A String value representing a player's inventory (using an
     * undocumented JSON format).
     * @param appid An integer representing the appid of the game with the
     * inventory.
     * @param contextid A long value representing the specific inventory context
     * (the 'sub-inventory' for a game, grouping items for an appid).
     */
    public TradeInternalInventory(JSONObject s, AppContextPair appContext) {
        this(s, appContext, new AssetBuilder());
    }

    public TradeInternalInventory(JSONObject json, AppContextPair appContext, AssetBuilder assetBuilder) {
        this.appContext = appContext;
        this.assetBuilder = assetBuilder;

        inventoryValid = false;

        inventoryItems = new ArrayList<>();
        currencyItems = new ArrayList<>();

        try {
            if (json.getBoolean("success")) {
                parseInventory(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * For large inventories, load additional inventory data.
     * @param json
     */
    public void loadMore(JSONObject json) {
        try {
            if (json.getBoolean("success")) {
                parseInventory(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the AppContextPair associated with the inventory. Example: A Team
     * Fortress 2 inventory would return an AppContextPair equal to
     * <code>new
     * AppContextPair(440, 2);</code>
     *
     * @return An AppContextPair "key" representing this instance.
     */
    public AppContextPair getAppContextPair() {
        return appContext;
    }

    /**
     * Gets the user's available trading inventory.
     *
     * @return A List containing all the available TradeInternalItem instances.
     */
    public List<TradeInternalItem> getItemList() {
        return inventoryItems;
    }

    /**
     * Gets the user's available currency items for the game.
     *
     * @return A List containing all available TradeInternalCurrency instances.
     */
    public List<TradeInternalCurrency> getCurrencyList() {
        return currencyItems;
    }

    /**
     * Returns whether or not the inventory loading was successful.
     *
     * @return
     */
    public boolean isValid() {
        return inventoryValid;
    }

    /**
     * Returns the error message associated with the error response.
     *
     * @return The JSON error message from the response if the inventory loading
     * was unsuccessful, or an empty string if it was.
     */
    public String getErrorMessage() {
        if (!inventoryValid) {
            return errorMessage;
        } else {
            return "";
        }
    }

    /**
     * Retrieves an item by its assetid.
     *
     * @param assetid The assetid of the TradeInternalItem to get.
     * @return A TradeInternalItem instance if available, or an instance of null
     * if not.
     */
    public TradeInternalItem getItem(long assetid) {
        for (TradeInternalItem item : inventoryItems) {
            if (item.assetid == assetid) {
                return item;
            }
        }
        return TradeInternalItem.UNAVAILABLE;
    }

    /**
     * Retrieves a currency item by its currencyid
     *
     * @param currencyid The currencyid of the TradeInternalCurrency to get.
     * @return A TradeInternalCurrency instance if available, or an instance of
     * null if not.
     */
    public TradeInternalCurrency getCurrency(long currencyid) {
        for (TradeInternalCurrency currency : currencyItems) {
            if (currency.currencyid == currencyid) {
                return currency;
            }
        }
        return null;
    }

    /**
     * Helper method to parse out the JSON inventory format.
     *
     * @param json JSONObject representing inventory to be parsed.
     * @throws JSONException
     */
    private void parseInventory(final JSONObject json) throws JSONException {
        inventoryValid = true;

        // Well. Something's fucky here.
        // They changed the 
        if (!json.getBoolean("success")) {
            //throw new Error("Retrieving inventory was unsuccessful.");
            inventoryValid = false;
            errorMessage = json.getString("error");
        }

        // Convenience map to associate class/instance to description.
        Map<ClassInstancePair, JSONObject> descriptions = new HashMap<>();
        JSONObject rgDescriptions = json.optJSONObject("rgDescriptions");
        for (final String rgDescriptionKey : (Set<String>) rgDescriptions.keySet()) {
            JSONObject rgDescriptionItem =
                    rgDescriptions.getJSONObject(rgDescriptionKey);

            int classid = rgDescriptionItem.getInt("classid");
            long instanceid = rgDescriptionItem.getLong("instanceid");

            descriptions.put(new ClassInstancePair(classid, instanceid),
                    rgDescriptionItem);
        }

        // Add non-currency items.
        JSONObject rgInventory = json.optJSONObject("rgInventory");
        if (rgInventory != null) {
            for (final String rgInventoryItem : (Set<String>) rgInventory.keySet()) {
                JSONObject invInstance =
                        rgInventory.getJSONObject(rgInventoryItem);

                ClassInstancePair itemCI = new ClassInstancePair(
                        Integer.parseInt(invInstance.getString("classid")),
                        Long.parseLong(invInstance.optString("instanceid", "0")));

                try {
                    TradeInternalItem generatedItem = assetBuilder.generateItem(appContext, invInstance, descriptions.get(itemCI));

                    inventoryItems.add(generatedItem);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // Add currency items
        JSONObject rgCurrency = json.optJSONObject("rgCurrency");
        if (rgCurrency != null) {
            for (final String rgCurrencyItem : (Set<String>) rgCurrency.keySet()) {
                JSONObject invInstance =
                        rgCurrency.getJSONObject(rgCurrencyItem);

                ClassInstancePair itemCI = new ClassInstancePair(
                        Integer.parseInt(invInstance.getString("classid")),
                        Long.parseLong(invInstance.optString("instanceid", "0")));

                try {
                    TradeInternalCurrency generatedItem =
                            assetBuilder.generateCurrency(appContext,
                            invInstance, descriptions.get(itemCI));

                    currencyItems.add(generatedItem);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Utility class to identify class-instance pairs.
     *
     * @author nosoop < nosoop at users.noreply.github.com >
     */
    protected static class ClassInstancePair {
        int classid;
        long instanceid;

        /**
         * Creates a class-instance pair.
         *
         * @param classid
         * @param instanceid
         */
        ClassInstancePair(int classid, long instanceid) {
            this.classid = classid;
            this.instanceid = instanceid;
        }

        @Override
        public int hashCode() {
            return 497 * classid + (int) instanceid;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ClassInstancePair other = (ClassInstancePair) obj;
            if (this.classid != other.classid) {
                return false;
            }
            if (this.instanceid != other.instanceid) {
                return false;
            }
            return true;
        }
    }

}