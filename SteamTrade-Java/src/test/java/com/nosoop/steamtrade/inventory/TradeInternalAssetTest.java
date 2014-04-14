/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nosoop.steamtrade.inventory;

import bundled.steamtrade.org.json.JSONException;
import bundled.steamtrade.org.json.JSONObject;
import bundled.steamtrade.org.json.JSONTokener;
import junit.framework.TestCase;

/**
 * A test for the TradeInternalAssetClass.
 * 
 * @author nosoop < nosoop at users.noreply.github.com >
 */
public class TradeInternalAssetTest extends TestCase {
    final TradeInternalInventory INVENTORY;
    final TradeInternalAsset ASSET_CURRENCY, ASSET_ITEM;

    public TradeInternalAssetTest(String testName) throws JSONException {
        super(testName);

        /**
         * Load inventory data from test resource.
         */
        final JSONObject INVENTORY_DATA =
                new JSONObject(new JSONTokener(
                TradeInternalAssetTest.class
                .getResourceAsStream("/inventorytest_99900.json")));
        /**
         * Use original AppContextPair data. (This is the same one used in my
         * personal inventory.)
         */
        final AppContextPair APP_CONTEXT = new AppContextPair(99900, 4062807L);
        /**
         * Use DEFAULT_ASSET_BUILDER as defined from TradeInternalInventories.
         */
        final AssetBuilder DEFAULT_ASSET_BUILDER =
                TradeInternalInventories.DEFAULT_ASSET_BUILDER;

        INVENTORY = new TradeInternalInventory(INVENTORY_DATA, APP_CONTEXT,
                DEFAULT_ASSET_BUILDER);

        ASSET_CURRENCY = INVENTORY.getCurrency(1L);
        ASSET_ITEM = INVENTORY.getItem(101220214L);
    }

    /**
     * Sets up the test, called before every test case method.
     *
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Tears down the test, called after every test case method.
     *
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getName method, of class TradeInternalAsset.
     */
    public void testGetName() {
        System.out.println("getName");
        assertEquals("Crowns", ASSET_CURRENCY.getName());
        assertEquals("Blue Shard", ASSET_ITEM.getName());
    }

    /**
     * Test of getMarketName method, of class TradeInternalAsset.
     */
    public void testGetMarketName() {
        System.out.println("getMarketName");
        assertEquals("", ASSET_CURRENCY.getMarketName());
        assertEquals("", ASSET_ITEM.getMarketName());
    }

    /**
     * Test of getAppid method, of class TradeInternalAsset.
     */
    public void testGetAppid() {
        System.out.println("getAppid");
        assertEquals(99900, ASSET_CURRENCY.getAppid());
        assertEquals(99900, ASSET_ITEM.getAppid());
    }

    /**
     * Test of getContextid method, of class TradeInternalAsset.
     */
    public void testGetContextid() {
        System.out.println("getContextid");
        assertEquals(4062807L, ASSET_CURRENCY.getContextid());
        assertEquals(4062807L, ASSET_ITEM.getContextid());
    }

    /**
     * Test of getAssetid method, of class TradeInternalAsset.
     */
    public void testGetAssetid() {
        System.out.println("getAssetid");
        assertEquals(1L, ASSET_CURRENCY.getAssetid());
        assertEquals(101220214L, ASSET_ITEM.getAssetid());
    }

    /**
     * Test of getClassid method, of class TradeInternalAsset.
     */
    public void testGetClassid() {
        System.out.println("getClassid");
        assertEquals(2407624, ASSET_CURRENCY.getClassid());
        assertEquals(4994733, ASSET_ITEM.getClassid());
    }

    /**
     * Test of getAmount method, of class TradeInternalAsset.
     */
    public void testGetAmount() {
        System.out.println("getAmount");
        assertEquals(550328, ASSET_CURRENCY.getAmount());
        assertEquals(281, ASSET_ITEM.getAmount());
    }

    /**
     * Test of getType method, of class TradeInternalAsset.
     */
    public void testGetType() {
        System.out.println("getType");
        assertEquals("Currency", ASSET_CURRENCY.getType());
        assertEquals("Material", ASSET_ITEM.getType());
    }
}
