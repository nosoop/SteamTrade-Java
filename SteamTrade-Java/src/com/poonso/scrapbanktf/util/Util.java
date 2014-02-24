package com.poonso.scrapbanktf.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class.  To be honest, I have no idea what's going on here.
 * (Replaced internal resource file with external file, added schema fallback 
 * so an error isn't thrown when the user adds an unknown item to the trade.)
 * 
 * @author Top-Cat, nosoop
 */
public class Util {

    private static Map<String, Response> webResponses = new HashMap<>();

    public static String webRequest(String url) {
        return webRequest(url, true);
    }

    public static String webRequest(String url, boolean useCache) {
        if (useCache && Util.webResponses.containsKey(url)) {
            if (Util.webResponses.get(url).isRecent()) {
                return Util.webResponses.get(url).getResponse();
            }
        }
        String out = "";
        try {
            final URL url2 = new URL(url);
            
            // Enabled support for compression.
            URLConnection curl = url2.openConnection();
            curl.setRequestProperty("Accept-Encoding", "gzip,deflate");
            
            java.io.InputStream netStream = curl.getInputStream();
            
            // If GZIPped response, then use the gzip decoder.
            if ("gzip".equals(curl.getContentEncoding())) {
                netStream = new java.util.zip.GZIPInputStream(netStream);
            }
            
            final BufferedReader reader = new BufferedReader(new InputStreamReader(netStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) {
                    out += "\n";
                }
                out += line;
            }
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        Util.webResponses.put(url, new Response(out));
        return out;
    }

    public static LinkedHashMap<Integer, MutableInt> sortHashMapByValues(HashMap<Integer, MutableInt> passedMap, boolean ascending) {
        final List<Integer> mapKeys = new ArrayList<>(passedMap.keySet());
        final List<MutableInt> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        if (!ascending) {
            Collections.reverse(mapValues);
        }

        final LinkedHashMap<Integer, MutableInt> someMap = new LinkedHashMap<>();
        final Iterator<MutableInt> valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            final MutableInt val = valueIt.next();
            final Iterator<Integer> keyIt = mapKeys.iterator();
            while (keyIt.hasNext()) {
                final Integer key = keyIt.next();
                if (passedMap.get(key).toString().equals(val.toString())) {
                    passedMap.remove(key);
                    mapKeys.remove(key);
                    someMap.put(key, val);
                    break;
                }
            }
        }
        return someMap;
    }
}