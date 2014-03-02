package com.nosoop.steamtrade;

import com.nosoop.steamtrade.inventory.TradeInternalItem;

public abstract class TradeListener {

    public TradeSession trade;

    /**
     * Defines trade status codes to be interpreted by the onError() method.
     */
    public class TradeStatusCodes {

        public final static int //
                /**
                 * Non-error statuses. Everything is okay according to Steam.
                 * Something weird is going on if onError() is called with these
                 * values.
                 */
                // We are polling for updates.
                STATUS_OK = 0,
                // Both users have decided to make the trade.
                TRADE_COMPLETED = 1,
                /**
                 * Steam web errors. Something funky happened on Steam's side.
                 * The error codes are defined by Steam.
                 */
                // One user cancelled.
                TRADE_CANCELLED = 3,
                // The other user timed out.
                PARTNER_TIMED_OUT = 4,
                // The trade failed in general.
                TRADE_FAILED = 5,
                /**
                 * SteamTrade-Java errors. Something in this library bugged out.
                 * The following error values are defined and used within the
                 * library.
                 */
                // There was a JSONException reached when parsing the status.
                STATUS_PARSE_ERROR = 1001,
                // The trade session was unable to fetch your inventories.
                BACKPACK_SCRAPE_ERROR = 1002;
    }

    /**
     * Called when an error occurs during the trade such that the trade is
     * closed.
     *
     * @param eid The error code. Known values are defined in
     * TradeListener.TradeErrorCodes.
     */
    public abstract void onError(int errorCode);

    /**
     * Called when the client polls the trade. If you want to warn the other
     * person for taking too long, implement this method and add a cancel.
     * Otherwise, just do nothing.
     */
    public abstract void onTimer(int secondsSinceAction, int secondsSinceTrade);

    /**
     * Called when the trade has opened.
     */
    public abstract void onWelcome();

    /**
     * Called after backpacks are loaded and trading can start. If you need to
     * store inventories in your own listener, handle that here.
     */
    public abstract void onAfterInit();

    /**
     * Called when the other person adds an item. If this is an item from a new
     * inventory, that inventory is loaded before this event is called.
     *
     * @param schemaItem
     * @param inventoryItem
     */
    public abstract void onUserAddItem(TradeInternalItem inventoryItem);

    /**
     * Called when the other person removes an item.
     *
     * @param schemaItem
     * @param inventoryItem
     */
    public abstract void onUserRemoveItem(TradeInternalItem inventoryItem);

    /**
     * Called when the other client send a message through Steam Trade.
     *
     * @param msg The received message.
     */
    public abstract void onMessage(String msg);

    /*
     * Called when the user checks / unchecks the 'ready' box.
     */
    public abstract void onUserSetReadyState(boolean ready);

    /**
     * Called once the trade is being processed after both players have accepted
     * (?).
     */
    public abstract void onUserAccept();

    /**
     * Called when something has happened in the trade.
     */
    public abstract void onNewVersion();

    /**
     * Called once a trade has been made.
     */
    public abstract void onTradeSuccess();

    /**
     * Called once the trade has been closed for the client to begin cleaning
     * up. Called immediately after a successful trade or trade error.
     */
    public abstract void onTradeClosed();
}
