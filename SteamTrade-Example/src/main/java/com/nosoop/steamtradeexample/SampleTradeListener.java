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

import bundled.steamtrade.org.json.JSONException;
import com.nosoop.steamtrade.TradeListener;
import com.nosoop.steamtrade.inventory.*;
import java.util.List;

/**
 * Example trade listener to show some of what is possible in a programming a
 * trade.
 *
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class SampleTradeListener extends TradeListener {

    /**
     * Event fired when the trade has encountered an error.
     *
     * @param errorCode The error code for the given error. Known values are
     * available as constants under TradeListener.TradeErrorCodes.
     */
    @Override
    public void onError(int errorCode, String msg) {
        String errorMessage;
        switch (errorCode) {
            case TradeStatusCodes.STATUS_ERRORMESSAGE:
                errorMessage = msg;
                break;
            case TradeStatusCodes.TRADE_CANCELLED:
                errorMessage = "The trade has been canceled.";
                break;
            case TradeStatusCodes.STATUS_PARSE_ERROR:
                errorMessage = "We have timed out.";
                break;
            case TradeStatusCodes.PARTNER_TIMED_OUT:
                errorMessage = "Other user timed out.";
                break;
            case TradeStatusCodes.TRADE_FAILED:
                errorMessage = "Trade failed.";
                break;
            default:
                errorMessage = "Unhandled error code " + errorCode + ".";
        }
        
        if (!msg.equals(TradeStatusCodes.EMPTY_MESSAGE)) {
            errorMessage += " (" + msg + ")";
        }

        System.out.println(errorMessage);
    }

    /**
     * Event fired every time the trade session is polled for updates to notify
     * the listener of how long we have been in the trade and how long it has
     * been since the trade partner's last input.
     *
     * Taking this approach over letting the software writer make their own
     * polling thread to keep things simple.
     *
     * @param secondsSinceAction
     * @param secondsSinceTrade
     */
    @Override
    public void onTimer(int secondsSinceAction, int secondsSinceTrade) {
        String message = String.format(
                "We have been in the trade for %d seconds. "
                + "It has been %d seconds since your last action.",
                secondsSinceTrade, secondsSinceAction);

        trade.getCmds().sendMessage(message);
        System.out.println(message);
    }

    /**
     * Called once everything but inventories have been initialized. (Originally
     * had to wait until all inventories were loaded, might as well keep it in
     * case.)
     */
    @Override
    public void onWelcome() {
        trade.getCmds().sendMessage("Hello!  Please wait while I figure out what items I have.");
    }

    /**
     * Called once everything is set. Show our inventory at our frontend, etc.
     */
    @Override
    public void onAfterInit() {
        trade.getCmds().sendMessage("Ready to trade!");
    }

    /**
     * Called when our trading partner has added an item.
     *
     * @param inventoryItem
     */
    @Override
    public void onUserAddItem(TradeInternalAsset inventoryItem) {
        trade.getCmds().sendMessage("You added a " + inventoryItem.getMarketName());

        if (inventoryItem instanceof TradeInternalItem) {
            TradeInternalItem item = (TradeInternalItem) inventoryItem;
            System.out.println(item.getName());
        }
    }

    /**
     * Called when our trading partner has removed an item.
     *
     * @param inventoryItem
     */
    @Override
    public void onUserRemoveItem(TradeInternalAsset inventoryItem) {
        trade.getCmds().sendMessage("You removed a " + inventoryItem.getMarketName());
    }

    /**
     * Called when our trading partner sent a message. In this example we will
     * add a random item whenever the other person says something.
     *
     * @param msg The message text.
     */
    @Override
    public void onMessage(String msg) {
        TradeInternalInventories itemStorage = trade.getSelf().getInventories();
        TradeInternalInventory tf2backpack = itemStorage.getInventory(440, 2);

        List<TradeInternalItem> tf2items = tf2backpack.getItemList();

        // Pick a random item from our TF2 inventory.
        TradeInternalItem item = tf2items.get((int) (Math.random() * tf2items.size()));
        trade.getCmds().addItem(item, 1);

        System.out.printf("User said %s and we put up a %s.%n", msg, item.getMarketName());
    }

    /**
     * Called when our trading partner ticked or unticked the "ready" checkbox.
     * In response, we will do the opposite of what they did so the trade never
     * happens, 50% of the time.
     *
     * @param ready Whether or not the checkbox is set.
     */
    @Override
    public void onUserSetReadyState(boolean ready) {
        System.out.println("User is ready: " + ready);

        if (Math.random() < .5)
            trade.getCmds().setReady(!ready);
    }

    /**
     * Called when the other user accepts the trade, in case you want to do
     * something about it.
     */
    @Override
    public void onUserAccept() {
        trade.getCmds().sendMessage("Hah. Nope. Cancelled.");
        
        // TODO Handle JSONException in the library.
        try {
            trade.getCmds().cancelTrade();
        } catch (JSONException ex) {
        }
    }

    /**
     * An event occurred. Normally wouldn't have to do anything here, but go
     * ahead and do something if you want.
     */
    @Override
    public void onNewVersion() {
        actionCount++;

        System.out.println(actionCount + " actions have been made during the trade.");
    }
    int actionCount = 0;

    /**
     * Called when the trade has been completed successfully.
     */
    @Override
    public void onTradeSuccess() {
        System.out.println("Items traded.");


        for (TradeInternalAsset item : trade.getSelf().getOffer()) {
            // TODO Provide example code to display what items were traded.
        }
    }

    /**
     * Called when the trade is done and we should stop polling for updates.
     * Remember, you can only be in one trade at a time (?), so you should be
     * telling the client that we are ready for another trade.
     */
    @Override
    public void onTradeClosed() {
        // Cleanup and whatnot.
    }
}
