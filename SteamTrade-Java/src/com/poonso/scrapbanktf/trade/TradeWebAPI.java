/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poonso.scrapbanktf.trade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to hold all web-based 'fetch' actions when dealing with Steam
 * Trade.
 *
 * @author nosoop
 */
public class TradeWebAPI {

    final String baseTradeURL;
    final String sessionId;
    final String steamLogin;

    public TradeWebAPI(String baseTradeURL, String sessionId, String steamLogin) {
        this.baseTradeURL = baseTradeURL;
        this.sessionId = sessionId;
        this.steamLogin = steamLogin;
    }

    public void addItem(int appid, long contextid, long assetid, int slot) {
        final Map<String, String> data = new HashMap<>();

        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        data.put("appid", "" + appid);
        data.put("contextid", "" + contextid);
        data.put("itemid", "" + assetid);
        data.put("slot", "" + slot);
        fetch(baseTradeURL + "additem", "POST", data);
    }

    public void removeItem(int appid, long contextid, long assetid) {
        final Map<String, String> data = new HashMap<>();
        
        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        data.put("appid", "" + appid);
        data.put("contextid", "" + contextid);
        data.put("itemid", "" + assetid);
        fetch(baseTradeURL + "removeitem", "POST", data);
    }
    
    public String sendMessage(String message) {
        final Map<String, String> data = new HashMap<>();
        try {
            data.put("sessionid", URLDecoder.decode(sessionId, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data.put("message", message);

        return fetch(baseTradeURL + "chat", "POST", data);
    }
    
    protected String fetch(String url, String method, Map<String, String> data) {
        return fetch(url, method, data, true);
    }

    protected String fetch(String url, String method, Map<String, String> data, boolean sendLoginData) {
        String cookies = "";
        if (sendLoginData) {
            try {
                cookies = "sessionid=" + URLEncoder.encode(sessionId, "UTF-8") + "; steamLogin=" + steamLogin + ";";
            } catch (UnsupportedEncodingException e) {
            }
        }
        final String response = request(url, method, data, cookies);
        return response;
    }
    
    // TODO patch up this thing too
    String request(String url, String method, Map<String, String> data, String cookies) {
        //String out = "";
        boolean ajax = true;
        StringBuilder out = new StringBuilder();
        try {
            String dataString = "";
            if (data != null) {
                for (final String key : data.keySet()) {
                    dataString += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(data.get(key), "UTF-8") + "&";
                }
            }
            if (!method.equals("POST")) {
                url += "?" + dataString;
            }
            final URL url2 = new URL(url);
            final HttpURLConnection conn = (HttpURLConnection) url2.openConnection();
            conn.setRequestProperty("Cookie", cookies);
            conn.setRequestMethod(method);
            System.setProperty("http.agent", "");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; Valve Steam Client/1392853084; ) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Safari/535.19");
            conn.setRequestProperty("Host", "steamcommunity.com");
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "text/javascript, text/hml, application/xml, text/xml, */*");
            
            // I don't know why, but we need a referer, otherwise we get a server error response.
            conn.setRequestProperty("Referer", baseTradeURL);
            
            // Accept compressed responses.  (We can decompress it.)
            conn.setRequestProperty("Accept-Encoding", "gzip,deflate");

            if (ajax) {
                conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
                conn.setRequestProperty("X-Prototype-Version", "1.7");
            }

            if (method.equals("POST")) {
                conn.setDoOutput(true);
                final OutputStreamWriter os = new OutputStreamWriter(conn.getOutputStream());
                os.write(dataString.substring(0, dataString.length()-1));
                os.flush();
            }

            java.io.InputStream netStream = conn.getInputStream();

            // If GZIPped response, then use the gzip decoder.
            if (conn.getContentEncoding().contains("gzip")) {
                netStream = new java.util.zip.GZIPInputStream(netStream);
            }

            //cookies = conn.getHeaderField("Set-Cookie");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(netStream));

            String line; // Stores an individual line currently being read.
            while ((line = reader.readLine()) != null) {
                if (out.length() > 0) {
                    out.append('\n');
                }
                out.append(line);
            }
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return out.toString();
    }
}
