package com.nosoop.steamtrade;

import com.nosoop.steamtrade.inventory.TradeInternalItem;

public abstract class TradeListener {

    public TradeSession trade;

    /**
     * Defines known trade error codes.
     */
    public class TradeErrorCodes {

        public final static int STATUS_ERROR = 1,
                TRADE_CANCELLED = 2,
                INITIALIZATION_ERROR = 3,
                PARTNER_TIMED_OUT = 4,
                TRADE_FAILED = 5;
    }
    
    /**
     * Called when an error occurs during the trade.
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
     * Called when the other person adds an item.
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
     * Called once the trade finishes.
     */
    public abstract void onComplete();
    
    
    public abstract void onTradeClosed();
}
