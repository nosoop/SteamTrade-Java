/*
 * The MIT License
 *
 * Copyright 2014 nosoop < nosoop at users.noreply.github.com >.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.nosoop.steamtradeexample;

import com.nosoop.steamtrade.TradeListener;
import com.nosoop.steamtrade.TradeSession;
import com.nosoop.steamtrade.inventory.AssetBuilder;
import com.nosoop.steamtradeasset.ROT13F2AssetBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Excerpt snippet to show use of SteamTrade-Java. In this case, we receive a
 * notification to start a trade from somewhere and we use the provided
 * TradeListener instance to start a trade.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class SteamClientExcerpt {
    /**
     * To access the trading page, we must know the following... -- The SteamID
     * of our currently signed-in user (in long value format) -- The
     * Base64-encoded current session identifier from Steam. -- The Steam login
     * token used for Steam Web services.
     *
     * Again, you'll probably want to use a reverse-engineered Steam library to
     * access this information and the notification to know when a trade session
     * is starting. It's probably the only way, actually, so. Eh.
     */
    long ourSteamId;
    String sessionid;
    String token;

    /**
     * Receives a callback notifying us that a trade has started.
     *
     * @param callback Callback with trade
     */
    public void onNotifiedTradeStart(MockSessionStartCallback callback) {
        TradeListener listener = new SampleTradeListener();
        TradeSession currentTrade; // The current trade.

        // Opens a new trade session to be handled by the given TradeListener.
        currentTrade = new TradeSession(
                ourSteamId, callback.tradePartnerSteamId,
                sessionid, token, listener);

        // Start a new thread in the background that polls the thread for updates.
        (new Thread(new TradePoller(currentTrade))).start();
    }

    /**
     * Receives a callback notifying us that a trade has started. We want to use
     * custom asset builders (see com.nosoop.steamtradeasset).
     *
     * @param callback Callback signaling trade.
     */
    public void onNotifiedTradeStartWithModifiedAssetBuilder(MockSessionStartCallback callback) {
        Map<Integer, AssetBuilder> assetBuilds = new HashMap<>();
        assetBuilds.put(440, new ROT13F2AssetBuilder());
        
        TradeListener listener = new SampleTradeListener();
        TradeSession currentTrade; // The current trade.

        // Added map at the end of the list.
        currentTrade = new TradeSession(
                ourSteamId, callback.tradePartnerSteamId,
                sessionid, token, listener, assetBuilds);

        // Start a new thread in the background that polls the thread for updates.
        (new Thread(new TradePoller(currentTrade))).start();
    }
}

/**
 * Mock callback data containing the minimum amount to start a trade session.
 */
class MockSessionStartCallback {
    long tradePartnerSteamId;
}

/**
 * Runnable that runs in an infinite loop to check updates. Continues to poll
 * even after the trade has closed. So don't use this.
 */
class TradePoller implements Runnable {
    TradeSession session;

    public TradePoller(TradeSession session) {
        this.session = session;
    }

    @Override
    public void run() {
        while (true) {
            session.run();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}