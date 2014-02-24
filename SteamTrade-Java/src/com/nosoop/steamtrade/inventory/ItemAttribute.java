package com.nosoop.steamtrade.inventory;

import org.json.simple.JSONObject;

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

    ItemAttribute(JSONObject obj) {
        defIndex = (short) (long) obj.get("defindex");
        value = String.valueOf(obj.get("value"));
        
        if (obj.containsKey("float_value")) {
            floatValue = (float) (double) obj.get("float_value");
        }
    }
}
