package com.nosoop.steamtrade.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.*;

/**
 * Represents a Steam user's inventory as displayed in trading and from viewing
 * an inventory online.
 *
 * @author nosoop (ported and adapted from ForeignInventory in the SteamBot
 * project by Jessecar96)
 */
public class TradeInternalInventory {

    boolean inventoryValid;
    List<TradeInternalItem> inventoryItems;
    // Debating on implementation for currency items.
    List<TradeInternalCurrency> currencyItems;
    int appid;
    long contextid;

    // {"amount":"12","timestamp":1376049909,"steamid":"76561198006980102","currencyid":"2","old_amount":"0","action":"6","appid":99900,"contextid":"4296569382"}
    // myInternalInventory.getInventory(99900, 4296569382).getCurrency(int currencyid); ??
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
    public TradeInternalInventory(String s, int appid, long contextid) {
        this.appid = appid;
        this.contextid = contextid;

        inventoryValid = false;

        try {
            JSONObject json = new JSONObject(s);

            if (json.getBoolean("success")) {
                parseInventory(json);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public AppContextPair getAppContextPair() {
        return new AppContextPair(appid, contextid);
    }

    public List<TradeInternalItem> getItemList() {
        return inventoryItems;
    }

    public TradeInternalItem getItem(long itemid) {
        for (TradeInternalItem item : inventoryItems) {
            if (item.assetid == itemid) {
                return item;
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

        inventoryItems = new ArrayList<>();
        currencyItems = new ArrayList<>();

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

                long id = Long.parseLong(invInstance.getString("id"));

                TradeInternalItem generatedItem =
                        new TradeInternalItem(id, descriptions.get(itemCI));

                generatedItem.appid = this.appid;
                generatedItem.contextid = this.contextid;

                inventoryItems.add(generatedItem);
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

                TradeInternalCurrency generatedItem =
                        new TradeInternalCurrency(
                        Integer.parseInt(invInstance.getString("id")),
                        descriptions.get(itemCI));

                generatedItem.appid = this.appid;
                generatedItem.contextid = this.contextid;

                generatedItem.amount = invInstance.getInt("amount");

                inventoryItems.add(generatedItem);
            }
        }
    }
}

/**
 * Utility class to identify class-instance pairs.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
class ClassInstancePair {

    int classid;
    long instanceid;

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
