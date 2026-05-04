/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for OsrsFlipPlugin core functionality.
 */
public class OsrsFlipPluginTest
{
private ProfitCalculator profitCalculator;
private CombinationRecipeManager recipeManager;
private ManualPriceManager manualPriceManager;
private HistoricalPriceTracker historicalTracker;

@Before
public void setUp()
{
profitCalculator = new ProfitCalculator();
recipeManager = new CombinationRecipeManager();
manualPriceManager = new ManualPriceManager();
historicalTracker = new HistoricalPriceTracker();
}

@Test
public void testCalculateBasicProfit_PositiveProfit()
{
int buyPrice = 100;
int sellPrice = 150;

int profit = profitCalculator.calculateBasicProfit(buyPrice, sellPrice);

assertEquals(47, profit);
}

@Test
public void testCalculateBasicProfit_NegativeProfit()
{
int buyPrice = 200;
int sellPrice = 150;

int profit = profitCalculator.calculateBasicProfit(buyPrice, sellPrice);

assertEquals(-53, profit);
}

@Test
public void testGetProfitThreshold_NoProfit()
{
assertEquals(ProfitThreshold.NONE, profitCalculator.getProfitThreshold(-1));
assertEquals(ProfitThreshold.NONE, profitCalculator.getProfitThreshold(0));
}

@Test
public void testGetProfitThreshold_LowProfit()
{
assertEquals(ProfitThreshold.LOW, profitCalculator.getProfitThreshold(1));
assertEquals(ProfitThreshold.LOW, profitCalculator.getProfitThreshold(49_999));
}

@Test
public void testGetProfitThreshold_MediumProfit()
{
assertEquals(ProfitThreshold.MEDIUM, profitCalculator.getProfitThreshold(50_000));
assertEquals(ProfitThreshold.MEDIUM, profitCalculator.getProfitThreshold(99_999));
}

@Test
public void testGetProfitThreshold_HighProfit()
{
assertEquals(ProfitThreshold.HIGH, profitCalculator.getProfitThreshold(100_000));
assertEquals(ProfitThreshold.HIGH, profitCalculator.getProfitThreshold(249_999));
}

@Test
public void testCombinationRecipe_AddIngredients()
{
CombinationRecipe recipe = new CombinationRecipe("Test Recipe", 1);

recipe.addIngredient(314, 2);
recipe.addIngredient(315, 1);

assertEquals(2, recipe.getIngredients().size());
assertEquals(2, recipe.getIngredientQuantity(314));
assertEquals(1, recipe.getIngredientQuantity(315));
}

@Test
public void testCombinationRecipe_Validate_Valid()
{
CombinationRecipe recipe = new CombinationRecipe("Valid Recipe", 1);
recipe.addIngredient(314, 2);
recipe.addIngredient(315, 1);

assertTrue(recipe.validate());
}

@Test
public void testCombinationRecipe_Validate_TooManyIngredients()
{
CombinationRecipe recipe = new CombinationRecipe("Too Many Ingredients", 1);

for (int i = 1; i <= 11; i++)
{
recipe.addIngredient(i, 1);
}

assertFalse(recipe.validate());
}

@Test
public void testManualPriceManager_SetAndGetPrices()
{
int itemId = 314;
int buyPrice = 100;
int sellPrice = 150;

manualPriceManager.setManualBuyPrice(itemId, buyPrice);
manualPriceManager.setManualSellPrice(itemId, sellPrice);

assertEquals(Integer.valueOf(buyPrice), manualPriceManager.getManualBuyPrice(itemId));
assertEquals(Integer.valueOf(sellPrice), manualPriceManager.getManualSellPrice(itemId));
}

@Test
public void testHistoricalPriceTracker_AddPricePoint()
{
int itemId = 314;

historicalTracker.addPricePoint(itemId, 100, 120);

assertEquals(1, historicalTracker.getHistory(itemId).size());
}

@Test
public void testHistoricalPriceTracker_MaxFiveEntries()
{
int itemId = 314;

for (int i = 1; i <= 7; i++)
{
historicalTracker.addPricePoint(itemId, i * 100, i * 120);
}

assertEquals(5, historicalTracker.getHistory(itemId).size());
}

@Test
public void testHistoricalPriceTracker_CalculateAverages()
{
int itemId = 314;

for (int i = 1; i <= 5; i++)
{
historicalTracker.addPricePoint(itemId, i * 100, i * 80);
}

double avgHigh = historicalTracker.getAverageHigh(itemId);
double avgLow = historicalTracker.getAverageLow(itemId);

assertEquals(300.0, avgHigh, 0.01);
assertEquals(240.0, avgLow, 0.01);
}
}
