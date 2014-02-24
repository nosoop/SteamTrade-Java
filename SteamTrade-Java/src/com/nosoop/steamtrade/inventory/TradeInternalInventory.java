package com.nosoop.steamtrade.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
            json = (JSONObject) new JSONParser().parse(s);

            if ((Boolean) json.get("success") == true) {
                inventoryValid = true;
                if ((json.get("rgInventory")) instanceof JSONObject) {
                    rgInventory = (JSONObject) (json.get("rgInventory"));
                }
                
                if ((json.get("rgDescriptions")) instanceof JSONObject) {
                    rgDescriptions = (JSONObject) (json.get("rgDescriptions"));
                }
                inventoryItems = new ArrayList<>();

                if (rgInventory != null) {
                    for (final Object rgInventoryItem : rgInventory.keySet()) {
                        generateInventoryItem(Long.parseLong((String) ((JSONObject) rgInventory.get(rgInventoryItem)).get("id")));
                    }
                }
                
                // TODO Add support for currency.
                /*if ((json.get("rgCurrency")) instanceof JSONObject) {
                    rgCurrency = (JSONObject) (json.get("rgCurrency"));
                    
                    Object rgCur = json.get("rgCurrency");
                    
                    for (final JSONObject rgCurrencyItem : (ArrayList<JSONObject>) rgCur) {
                        generateCurrencyItem(Long.parseLong((String) ((JSONObject) rgCurrency.get(rgCurrencyItem)).get("id")));
                    }
                }*/
                if (json.get("rgCurrency") instanceof HashMap<?, ?>) {
                    Iterator<Map.Entry<String, JSONObject>> iterator;
                    iterator = ((HashMap<String, JSONObject>) json.get("rgCurrency")).entrySet().iterator();
                    while (iterator.hasNext()) {
                        final Map.Entry<String, JSONObject> row = iterator.next();
                        generateCurrencyItem(Long.parseLong(row.getKey()));
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private int getClassIdForItemId(long itemid) {
        String i = itemid + "";

        return Integer.parseInt((String) ((JSONObject) rgInventory.get(i)).get("classid"));
    }

    private long getInstanceIdForItemId(long itemid) {
        String i = itemid + "";

        if (!((JSONObject) rgInventory.get(i)).containsKey("instanceid")) {
            return 0;
        }

        return Long.parseLong((String) (((JSONObject) rgInventory.get(i)).get("instanceid")));
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
        //TradeInternalItem newItem = generateInventoryItem(itemid);
        //inventoryItems.add(newItem);
        //return newItem;
        return null;
    }
    
    private void generateCurrencyItem(long currencyid) {
        long classid = Long.parseLong((String) (rgCurrency.get(currencyid + "")));
        int instanceid = 0; // ?
        
        String index = String.format("%d_%d", classid, instanceid);
        
        TradeInternalCurrency generatedItem = new TradeInternalCurrency(currencyid, (JSONObject) rgDescriptions.get(index));
        
        currencyItems.add(generatedItem);
    }

    private void generateInventoryItem(long itemid) {
        int classid = getClassIdForItemId(itemid);
        long instanceid = getInstanceIdForItemId(itemid);

        String index = String.format("%d_%d", classid, instanceid);

        long id = Long.parseLong((String) ((JSONObject) rgInventory.get(itemid + "")).get("id"));

        TradeInternalItem generatedItem = new TradeInternalItem(id, (JSONObject) rgDescriptions.get(index));

        generatedItem.appid = this.appid;
        generatedItem.contextid = this.contextid;

        inventoryItems.add(generatedItem);
    }
}
