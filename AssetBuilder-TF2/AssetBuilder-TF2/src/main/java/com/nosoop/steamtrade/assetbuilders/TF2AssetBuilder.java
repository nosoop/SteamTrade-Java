/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.assetbuilders;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;
import com.nosoop.steamtrade.inventory.AppContextPair;
import com.nosoop.steamtrade.inventory.AssetBuilder;
import com.nosoop.steamtrade.inventory.TradeInternalItem;

/**
 * An asset builder that generates TF2Item instances.
 * 
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class TF2AssetBuilder extends AssetBuilder {
    @Override
    public TradeInternalItem generateItem(AppContextPair appContext, JSONObject rgInventory, JSONObject rgDescription) throws JSONException {
        return new TF2Item(appContext, rgInventory, rgDescription);
    }
}
