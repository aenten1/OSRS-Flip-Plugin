/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.osrsflip;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("osrsflip")
public interface OsrsFlipConfig extends Config
{
	@ConfigSection(
		name = "Layout",
		description = "Panel layout settings",
		position = 0
	)
	String layoutSection = "layout";

	@ConfigSection(
		name = "Prices",
		description = "Price source and calculation settings",
		position = 1
	)
	String pricesSection = "prices";

	@ConfigSection(
		name = "Offsets",
		description = "Adjust buy/sell prices by a fixed amount or percentage",
		position = 2
	)
	String offsetsSection = "offsets";

	// --- Layout ---

	@ConfigItem(
		keyName = "combinationsFirst",
		name = "Show Combinations First",
		description = "When enabled, the Combinations tab is shown before the Watchlist tab",
		section = layoutSection,
		position = 0
	)
	default boolean combinationsFirst()
	{
		return true;
	}

	// --- Prices ---

	@ConfigItem(
		keyName = "autoRefresh",
		name = "Auto-Refresh Prices",
		description = "Automatically refresh all prices on a timer",
		section = pricesSection,
		position = 0
	)
	default boolean autoRefresh()
	{
		return true;
	}

	@ConfigItem(
		keyName = "autoRefreshMinutes",
		name = "Refresh Interval (min)",
		description = "How often to auto-refresh prices, in minutes",
		section = pricesSection,
		position = 1
	)
	default int autoRefreshMinutes()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "useWikiPrices",
		name = "Use Wiki Prices",
		description = "Fetch real-time prices from the OSRS Wiki API instead of RuneLite's cache",
		section = pricesSection,
		position = 2
	)
	default boolean useWikiPrices()
	{
		return true;
	}

	@ConfigItem(
		keyName = "priceMode",
		name = "Price Mode",
		description = "Which price fields to use for buy/sell calculations",
		section = pricesSection,
		position = 1
	)
	default PriceMode priceMode()
	{
		return PriceMode.BUY_LOW_SELL_HIGH;
	}

	@ConfigItem(
		keyName = "volatilityThreshold",
		name = "Volatility Threshold (%)",
		description = "If latest price deviates from 1h average by more than this %, use the 1h average instead",
		section = pricesSection,
		position = 2
	)
	default int volatilityThreshold()
	{
		return 15;
	}

	// --- Offsets ---

	@ConfigItem(
		keyName = "buyOffsetGp",
		name = "Buy Offset (GP)",
		description = "Fixed GP amount added to buy price (e.g. 5000 to buy 5k above market)",
		section = offsetsSection,
		position = 0
	)
	default int buyOffsetGp()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "buyOffsetPercent",
		name = "Buy Offset (%)",
		description = "Percentage added to buy price (e.g. 0.5 to buy 0.5% above market)",
		section = offsetsSection,
		position = 1
	)
	default double buyOffsetPercent()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "sellOffsetGp",
		name = "Sell Offset (GP)",
		description = "Fixed GP amount added to sell price (e.g. -5000 to sell 5k below market)",
		section = offsetsSection,
		position = 2
	)
	default int sellOffsetGp()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "sellOffsetPercent",
		name = "Sell Offset (%)",
		description = "Percentage added to sell price (e.g. -0.5 to sell 0.5% below market)",
		section = offsetsSection,
		position = 3
	)
	default double sellOffsetPercent()
	{
		return 0.0;
	}
}
