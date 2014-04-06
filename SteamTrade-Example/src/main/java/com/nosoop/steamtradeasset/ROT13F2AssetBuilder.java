/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtradeasset;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.AssetBuilder;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

/**
 * Implementation of a silly little asset builder.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class ROT13F2AssetBuilder extends AssetBuilder {
    /**
     * Generates a TradeInternalItem, given a game's app-context pair, a
     * JSONObject representing the inventory data for the item, and a JSONObject
     * representing the description for the item.
     *
     * @param appContext
     * @param rgInventory
     * @param rgDescription
     * @return
     * @throws JSONException
     */
    @Override
    public TradeInternalItem generateItem(AppContextPair appContext, JSONObject rgInventory, JSONObject rgDescription) throws JSONException {
        /**
         * Uses the ROT13 cipher on the name and puts it back into the
         * description object to be used in constructing the TradeInternalItem
         * instance.
         */
        rgDescription.put("name", rot13(rgDescription.getString("name")));

        /**
         * One can also subclass TradeInternalItem and TradeInternalCurrency to
         * add new fields and call its constructor instead.
         */
        return new TradeInternalItem(appContext, rgInventory, rgDescription);
    }

    public static String rot13(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 'a' && c <= 'm') {
                c += 13;
            } else if (c >= 'A' && c <= 'M') {
                c += 13;
            } else if (c >= 'n' && c <= 'z') {
                c -= 13;
            } else if (c >= 'N' && c <= 'Z') {
                c -= 13;
            }
            output.append(c);
        }
        return output.toString();
    }
}
