/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

public enum PriceMode
{
	BUY_LOW_SELL_HIGH("Buy Low / Sell High"),
	BUY_HIGH_SELL_LOW("Buy High / Sell Low"),
	BUY_HIGH_SELL_HIGH("Buy High / Sell High");

	private final String label;

	PriceMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
