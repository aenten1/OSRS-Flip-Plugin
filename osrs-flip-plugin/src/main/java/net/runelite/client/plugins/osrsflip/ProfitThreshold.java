/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

/**
 * Profit threshold categories for flip recommendations.
 */
public enum ProfitThreshold
{
NONE(0),
LOW(50_000),
MEDIUM(100_000),
HIGH(250_000),
VERY_HIGH(500_000),
ULTRA_HIGH(1_000_000),
LEGENDARY(1_000_000);

private final int minimumProfit;

ProfitThreshold(int minimumProfit)
{
this.minimumProfit = minimumProfit;
}

public int getMinimumProfit()
{
return minimumProfit;
}
}
