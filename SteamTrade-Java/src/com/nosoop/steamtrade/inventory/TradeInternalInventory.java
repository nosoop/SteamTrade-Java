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
    JSONObject json;
    JSONObject rgInventory = null;
    JSONObject rgDescriptions = null;
    JSONObject rgCurrency = null;
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
            json = new JSONObject(s);

            if (json.getBoolean("success")) {
                inventoryValid = true;
                
                rgInventory = json.optJSONObject("rgInventory");
                rgDescriptions = json.optJSONObject("rgDescriptions");
                
                inventoryItems = new ArrayList<>();
                currencyItems = new ArrayList<>();

                if (rgInventory != null) {
                    for (final String rgInventoryItem : (Set<String>) rgInventory.keySet()) {
                        JSONObject itemEntry = rgInventory.getJSONObject(rgInventoryItem);
                        
                        generateInventoryItem(Long.parseLong(itemEntry.getString("id")));
                    }
                }
                
                rgCurrency = json.optJSONObject("rgCurrency");
                if (rgCurrency != null) {
                    for (final String rgCurrencyItem : (Set<String>) rgCurrency.keySet()) {
                        JSONObject itemEntry = rgCurrency.getJSONObject(rgCurrencyItem);
                        
                        generateCurrencyItem(Long.parseLong(itemEntry.getString("classid")));
                    }
                }
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
    
    private void generateCurrencyItem(long currencyid) throws JSONException {
        int instanceid = 0; // ?
        
        String index = String.format("%d_%d", currencyid, instanceid);
        
        System.out.println(rgDescriptions.getJSONObject(index));
        
        TradeInternalCurrency generatedItem = new TradeInternalCurrency(currencyid, rgDescriptions.getJSONObject(index));
        
        currencyItems.add(generatedItem);
    }

    private void generateInventoryItem(long itemid) throws JSONException {
        String i = String.valueOf(itemid);
        JSONObject invInstance = rgInventory.getJSONObject(i);
        
        int classid = Integer.parseInt(invInstance.getString("classid"));
        long instanceid = Long.parseLong(invInstance.optString("instanceid", "0"));

        String index = String.format("%d_%d", classid, instanceid);

        long id = Long.parseLong(invInstance.getString("id"));

        TradeInternalItem generatedItem = new TradeInternalItem(id, rgDescriptions.getJSONObject(index));

        generatedItem.appid = this.appid;
        generatedItem.contextid = this.contextid;

        inventoryItems.add(generatedItem);
    }
}
