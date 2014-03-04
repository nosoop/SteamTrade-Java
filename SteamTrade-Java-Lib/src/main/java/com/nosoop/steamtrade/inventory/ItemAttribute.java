package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;

/**
 * Class that holds individual item attributes.
 * Modified to support the "float_value" value, which is required to determine 
 * which crate series is what, among other things.
 * 
 * @author Top-Cat, nosoop
 */
public class ItemAttribute {

    public short defIndex;
    public float floatValue;
    public String value;

    @Deprecated
    ItemAttribute(JSONObject obj) throws JSONException {
        defIndex = (short) obj.getInt("defindex");
        value = obj.getString("value");
        
        if (obj.has("float_value")) {
            floatValue = (float) obj.getDouble("float_value");
        }
    }
}
