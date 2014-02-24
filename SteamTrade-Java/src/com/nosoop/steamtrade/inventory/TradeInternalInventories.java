package com.nosoop.steamtrade.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a user's collection of game inventories, retrievable by the
 * inventory-specific appid and contextid.
 *
 * @author nosoop
 */
public class TradeInternalInventories {

    Map<AppContextPair, TradeInternalInventory> gameInventories;

    public TradeInternalInventories() {
        gameInventories = new HashMap<>();
    }

    /**
     * Adds a new inventory to the collection using an unnamed AppContextPair.
     *
     * @param appid The game's appid.
     * @param contextid The inventory's contextid.
     * @param feed The backpack's JSON data.
     */
    public void addInventory(int appid, long contextid, String feed) {
        gameInventories.put(getInventoryKey(appid, contextid), new TradeInternalInventory(feed, appid, contextid));
    }

    /**
     * Adds a new inventory to the collection using a given AppContextPair.
     *
     * @param appContext
     * @param feed
     */
    public void addInventory(AppContextPair appContext, String feed) {
        gameInventories.put(appContext, new TradeInternalInventory(feed, appContext.appid, appContext.contextid));
    }

    /**
     * Returns a boolean value stating if the inventory collection contains a
     * specific inventory.
     *
     * @param appid A game's appid.
     * @param contextid An game's inventory contextid.
     * @return Whether or not the inventory map contains a key value 
     * AppContextPair represented by the given appid and contextid.
     */
    public boolean hasInventory(int appid, long contextid) {
        return gameInventories.containsKey(getInventoryKey(appid, contextid));
    }

    /**
     * Returns a boolean value stating if the inventory collection contains a
     * specific inventory.
     *
     * @param contextdata An AppContextPair 
     * @return Whether or not the inventory map contains a key value of the 
     * given AppContextPair.
     */
    public boolean hasInventory(AppContextPair contextdata) {
        return gameInventories.containsKey(contextdata);
    }

    /**
     * @param appid
     * @param contextid
     * @return TradeInternalInventory for the given appid and contextid.
     */
    public TradeInternalInventory getInventory(int appid, long contextid) {
        return gameInventories.get(getInventoryKey(appid, contextid));
    }

    /**
     * @param contextdata
     * @return TradeInternalInventory for the given AppContextPair.
     */
    public TradeInternalInventory getInventory(AppContextPair contextdata) {
        return gameInventories.get(contextdata);
    }

    /**
     * @param appid
     * @param contextid
     * @return An unnamed AppContextPair.
     */
    private AppContextPair getInventoryKey(int appid, long contextid) {
        return new AppContextPair(appid, contextid);
    }
    
    /**
     * @return A list of all available / known inventories held by this object.
     */
    public List<TradeInternalInventory> getInventories() {
        return new ArrayList<>(gameInventories.values());
    }
}
