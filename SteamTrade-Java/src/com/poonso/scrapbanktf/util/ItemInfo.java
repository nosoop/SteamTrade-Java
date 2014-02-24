package com.poonso.scrapbanktf.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Holds item information as received during the trade.
 * (Removed the now-broken support for crate series numbers, modified to 
 * support the new IEconItems_440 API, exposes itemClass.)
 * 
 * @author Top-Cat
 */
public class ItemInfo {

    private final String name;
    private final String itemClass;
    private final Map<Short, Double> attributes = new HashMap<>();

    @SuppressWarnings("unchecked")
    public ItemInfo(int itemid, JSONObject info) {
        itemClass = (String) info.get("item_class");
        
        // item_name is fine as long as you're using a supported language.
        // http://api.steampowered.com/IEconItems_440/GetSchema/v0001/?key=%s&language=en
        name = (String) info.get("item_name");
        
        if (info.containsKey("attributes")) {
            if (info.get("attributes") instanceof JSONArray) {
                final JSONArray arr = (JSONArray) info.get("attributes");
                for (final JSONObject obj : (List<JSONObject>) arr) {
                    addAttribute(obj);
                }
            } else {
                for (final Entry<String, JSONObject> attr : (Set<Entry<String, JSONObject>>) ((JSONObject) info.get("attributes")).entrySet()) {
                    addAttribute(attr.getValue());
                }
            }
        }
    }

    private void addAttribute(JSONObject obj) {
        final Object val = obj.get("value");
        
        // Use attribute's defindex for compactness.
        short defIndex = (short) Schema.getClassDefIndex((String) obj.get("class"));
        
        if (val instanceof Long) {
            attributes.put(defIndex, (double) (long) val);
        } else if (val instanceof Double) {
            attributes.put(defIndex, (double) val);
        } else {
            // DebugPrint.println("Could not retrieve item attribute value.");
        }
    }

    public String getName() {
        return name;
    }
    
    public String getItemClass() {
        return itemClass;
    }
}
