/*
 * Copyright (c) 2026, Aaron Enten (ace_554)
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
import net.runelite.client.config.Range;

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
		position = 2,
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean useWikiPrices()
	{
		return false;
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

	@Range(min = -1000000000)
	@ConfigItem(
		keyName = "buyOffsetGp",
		name = "Buy Offset (GP)",
		description = "GP added to buy price. Negative to lower.",
		section = offsetsSection,
		position = 0
	)
	default int buyOffsetGp()
	{
		return 0;
	}

	@Range(min = -100)
	@ConfigItem(
		keyName = "buyOffsetPercent",
		name = "Buy Offset (%)",
		description = "Percent added to buy price. Negative to lower.",
		section = offsetsSection,
		position = 1
	)
	default int buyOffsetPercent()
	{
		return 0;
	}

	@Range(min = -1000000000)
	@ConfigItem(
		keyName = "sellOffsetGp",
		name = "Sell Offset (GP)",
		description = "GP added to sell price. Negative to lower.",
		section = offsetsSection,
		position = 2
	)
	default int sellOffsetGp()
	{
		return 0;
	}

	@Range(min = -100)
	@ConfigItem(
		keyName = "sellOffsetPercent",
		name = "Sell Offset (%)",
		description = "Percent added to sell price. Negative to lower.",
		section = offsetsSection,
		position = 3
	)
	default int sellOffsetPercent()
	{
		return 0;
	}

	@Range(min = -1000000000)
	@ConfigItem(
		keyName = "buyRoundInterval",
		name = "Buy Round (GP)",
		description = "Round buy price to nearest interval. Positive = up, negative = down. 0 = off.",
		section = offsetsSection,
		position = 4
	)
	default int buyRoundInterval()
	{
		return 0;
	}

	@Range(min = -1000000000)
	@ConfigItem(
		keyName = "sellRoundInterval",
		name = "Sell Round (GP)",
		description = "Round sell price to nearest interval. Positive = up, negative = down. 0 = off.",
		section = offsetsSection,
		position = 5
	)
	default int sellRoundInterval()
	{
		return 0;
	}

}
