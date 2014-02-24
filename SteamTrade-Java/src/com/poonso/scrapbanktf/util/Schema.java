package com.poonso.scrapbanktf.util;

import com.poonso.scrapbanktf.util.ItemInfo;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
//
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * A utility class to store the item schema. (Extracted from its original
 * location of [...].scrapbanktf.util.Util, replaced resource stream with file,
 * and modified to support IEconItems_440. Also added support to retrieve
 * quality names.)
 * 
 * To be honest, there really isn't any need for this class anymore.
 *
 * @author Top-Cat, nosoop
 */
public class Schema {

    private static Map<Integer, ItemInfo> itemInfo = new HashMap<>();
    private static final Map<Integer, JSONObject> items;
    private static final Map<String, Short> attrDefs;
    
    private static final String[] qualityNameIndex;

    static {
        items = new HashMap<>();
        attrDefs = new HashMap<>();
        qualityNameIndex = new String[32];
        
        File schemaFile = new File("tf_schema.json");

        if (schemaFile.exists()) {
            loadSchema(schemaFile);
        }
    }
    
    /**
     * Utility class to offload schema loading.
     *
     * @param schemaFile
     */
    private static void loadSchema(File schemaFile) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(schemaFile)));
            JSONObject itemSchema = (JSONObject) new JSONParser().parse(reader);

            JSONObject result = ((JSONObject) itemSchema.get("result"));

            // Maps integer definitions to items, used in [...] util.ItemInfo
            JSONArray feedItems = (JSONArray) result.get("items");
            for (Object item : feedItems) {
                JSONObject jsItem = (JSONObject) item;
                items.put((int) (long) jsItem.get("defindex"), jsItem);
            }

            // Maps attribute class names to their integer definition.
            JSONArray attributes = (JSONArray) result.get("attributes");
            for (Object item : attributes) {
                JSONObject jsItem = (JSONObject) item;
                attrDefs.put((String) jsItem.get("attribute_class"),
                        (short) (long) jsItem.get("defindex"));
            }
            
            JSONObject qualities = (JSONObject) result.get("qualities");
            JSONObject qualityNames = (JSONObject) result.get("qualityNames");
            for (Object quality : qualities.keySet()) {
                qualityNameIndex[(int) (long) (qualities.get(quality))] = (String) qualityNames.get(quality);
            }

        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final ParseException e) {
            e.printStackTrace();
        }
    }

    public static ItemInfo getItemInfo(int itemdefindex) {
        if (!itemInfo.containsKey(itemdefindex)) {
            if (items.containsKey(itemdefindex)) {
                // Item exists in the schema.
                itemInfo.put(itemdefindex, new ItemInfo(itemdefindex, (JSONObject) items.get(itemdefindex)));
            } else {
                // Item doesn't exist in the current schema...
                // Use a basic fallback.
                itemInfo.put(itemdefindex, new ItemInfo(itemdefindex, EmptyItem.getEmptyItem(itemdefindex)));
            }
        }
        return itemInfo.get(itemdefindex);
    }

    /**
     * Gets the defindex of an attribute based on the class name.
     *
     * @param attributeClass The classname of the attribute.
     * @return The defindex of the input attributeClass, or -1 if not found.
     */
    public static int getClassDefIndex(String attributeClass) {
        if (attrDefs.containsKey(attributeClass)) {
            return attrDefs.get(attributeClass);
        } else {
            return -1;
        }
    }
    
    public static String getQualityName(byte quality) {
        return qualityNameIndex[quality];
    }
}

/**
 * A generic JSONObject to use when the item is currently undefined.
 *
 * @author nosoop
 */
class EmptyItem {

    static final JSONObject emptyItem;
    final static String emptyItemName = "(Unknown Item %d)";

    static {
        emptyItem = new JSONObject();
        emptyItem.put("item_class", "unknown_item");
    }

    public static JSONObject getEmptyItem(int itemid) {
        JSONObject item = new JSONObject(emptyItem);
        item.put("item_name", String.format(emptyItemName, itemid));
        return item;
    }
}