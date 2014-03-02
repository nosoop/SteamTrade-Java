package com.nosoop.steamtradeexample;

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
    public void onError(int errorCode) {
        String errorMessage;
        switch (errorCode) {
            case TradeStatusCodes.STATUS_ERROR:
                errorMessage = "Error parsing trade status.";
                break;
            case TradeStatusCodes.TRADE_CANCELLED:
                errorMessage = "The trade has been canceled.";
                break;
            case TradeStatusCodes.INITIALIZATION_ERROR:
                errorMessage = "We have timed out.";
                break;
            case TradeStatusCodes.PARTNER_TIMED_OUT:
                errorMessage = "Other user timed out.";
                break;
            case TradeStatusCodes.TRADE_FAILED:
                errorMessage = "Trade failed.";
                break;
            default:
                errorMessage = "Unknown error (eid:" + errorCode + ").";
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
    public void onUserAddItem(TradeInternalItem inventoryItem) {
        trade.getCmds().sendMessage("You added a " + inventoryItem.marketName);

        if (inventoryItem.isRenamed) {
            System.out.println(inventoryItem.marketName);
            System.out.println("  - named " + inventoryItem.displayName);
        }
    }

    /**
     * Called when our trading partner has removed an item.
     *
     * @param inventoryItem
     */
    @Override
    public void onUserRemoveItem(TradeInternalItem inventoryItem) {
        trade.getCmds().sendMessage("You removed a " + inventoryItem.marketName);
    }

    /**
     * Called when our trading partner sent a message. In this example we will
     * add a random item whenever the other person says something.
     *
     * @param msg The message text.
     */
    @Override
    public void onMessage(String msg) {
        TradeInternalInventories itemStorage = trade.myTradeInventories;
        TradeInternalInventory tf2backpack = itemStorage.getInventory(440, 2);

        List<TradeInternalItem> tf2items = tf2backpack.getItemList();

        // Pick a random item from our TF2 inventory.
        TradeInternalItem item = tf2items.get((int) (Math.random() * tf2items.size()));
        trade.getCmds().addItem(item, 1);

        System.out.printf("User said %s and we put up a %s.\n", msg, item.marketName);
    }

    /**
     * Called when our trading partner ticked or unticked the "ready" checkbox.
     * In response, we will do the opposite of what they did so the trade never
     * happens.
     *
     * @param ready Whether or not the checkbox is set.
     */
    @Override
    public void onUserSetReadyState(boolean ready) {
        System.out.println("User is ready: " + ready);

        trade.getCmds().setReady(!ready);
    }

    /**
     * I think this is an event called when the other user accepts the trade? I
     * have no idea. Going to have to take a look again.
     */
    @Override
    public void onUserAccept() {
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

        // TODO Provide example code to display what items were traded.
    }

    /**
     * Called when the trade is done and we should stop polling for updates.
     */
    @Override
    public void onTradeClosed() {
        // Cleanup and whatnot.
    }
}
