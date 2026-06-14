/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

/**
 * Calculates profits for item flips and combinations.
 * GE tax is 2% of the sell price, floored to the nearest whole number,
 * capped at 5,000,000 gp. Items sold below 50 gp have no tax.
 */
public class ProfitCalculator
{
	private static final double GE_TAX_RATE = 0.02;
	private static final int GE_TAX_CAP = 5_000_000;
	private static final int GE_TAX_THRESHOLD = 50;

	/**
	 * Calculates the GE tax for a given sell price.
	 */
	public int calculateTax(int sellPrice)
	{
		if (sellPrice < GE_TAX_THRESHOLD)
		{
			return 0;
		}

		int tax = (int) Math.floor(sellPrice * GE_TAX_RATE);
		return Math.min(tax, GE_TAX_CAP);
	}

	/**
	 * Calculates profit for a basic flip (buy low, sell high with tax).
	 */
	public int calculateBasicProfit(int buyPrice, int sellPrice)
	{
		if (buyPrice < 0 || sellPrice < 0)
		{
			return -1;
		}

		int tax = calculateTax(sellPrice);
		return sellPrice - tax - buyPrice;
	}

	/**
	 * Calculates profit for a combination recipe.
	 */
	public int calculateCombinationProfit(CombinationRecipe recipe, int totalIngredientCost, int combinedSellPrice)
	{
		if (totalIngredientCost < 0 || combinedSellPrice < 0)
		{
			return -1;
		}

		int tax = calculateTax(combinedSellPrice);
		return combinedSellPrice - tax - totalIngredientCost;
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
