/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

/**
 * Calculates profits for item flips and combinations.
 */
public class ProfitCalculator
{
private static final double GE_TAX_RATE = 0.02;

/**
 * Calculates profit for a basic flip (buy low, sell high with 2% tax).
 * Formula: profit = (sellPrice * 0.98) - buyPrice
 */
public int calculateBasicProfit(int buyPrice, int sellPrice)
{
if (buyPrice < 0 || sellPrice < 0)
{
return -1;
}

int taxedSellPrice = (int) Math.floor(sellPrice * (1 - GE_TAX_RATE));
return taxedSellPrice - buyPrice;
}

/**
 * Calculates profit for a combination recipe.
 * Formula: profit = (combinedSellPrice * 0.98) - totalIngredientCost
 */
public int calculateCombinationProfit(CombinationRecipe recipe, int totalIngredientCost, int combinedSellPrice)
{
if (totalIngredientCost < 0 || combinedSellPrice < 0)
{
return -1;
}

int taxedSellPrice = (int) Math.floor(combinedSellPrice * (1 - GE_TAX_RATE));
return taxedSellPrice - totalIngredientCost;
}

/**
 * Returns the profit threshold category for a given profit amount.
 */
public ProfitThreshold getProfitThreshold(int profit)
{
if (profit <= 0)
{
return ProfitThreshold.NONE;
}
else if (profit < 50_000)
{
return ProfitThreshold.LOW;
}
else if (profit < 100_000)
{
return ProfitThreshold.MEDIUM;
}
else if (profit < 250_000)
{
return ProfitThreshold.HIGH;
}
else if (profit < 500_000)
{
return ProfitThreshold.VERY_HIGH;
}
else if (profit < 1_000_000)
{
return ProfitThreshold.ULTRA_HIGH;
}
else
{
return ProfitThreshold.LEGENDARY;
}
}
}
