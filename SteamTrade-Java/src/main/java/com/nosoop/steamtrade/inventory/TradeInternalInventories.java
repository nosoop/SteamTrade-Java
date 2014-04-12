package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONObject;
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
    List<AssetBuilder> inventoryLoaders;
    final static AssetBuilder DEFAULT_ASSET_BUILDER = new AssetBuilder() {
        @Override
        public boolean isSupported(AppContextPair appContext) {
            return true;
        }
    };

    public TradeInternalInventories() {
        this(new ArrayList<AssetBuilder>());
    }

    public TradeInternalInventories(List<AssetBuilder> assetBuild) {
        this.inventoryLoaders = assetBuild;
        this.gameInventories = new HashMap<>();
    }

    /**
     * Adds a new inventory to the collection using a given AppContextPair.
     *
     * @param appContext
     * @param feed
     */
    public void addInventory(AppContextPair appContext, JSONObject feed) {
        AssetBuilder asset = DEFAULT_ASSET_BUILDER;

        for (AssetBuilder build : inventoryLoaders) {
            if (build.isSupported(appContext)) {
                asset = build;
                break;
            }
        }

        gameInventories.put(appContext,
                new TradeInternalInventory(feed, appContext, asset));
    }

    public void addInventory(AppContextPair appContext) {
        AssetBuilder asset = DEFAULT_ASSET_BUILDER;

        for (AssetBuilder build : inventoryLoaders) {
            if (build.isSupported(appContext)) {
                asset = build;
                break;
            }
        }

        gameInventories.put(appContext,
                new TradeInternalInventory(appContext, asset));
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
        return hasInventory(getInventoryKey(appid, contextid));
    }

    /**
     * Returns a boolean value stating if the inventory collection contains a
     * specific inventory.
     *
     * @param contextdata An AppContextPair representing a game inventory to
     * check for.
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
