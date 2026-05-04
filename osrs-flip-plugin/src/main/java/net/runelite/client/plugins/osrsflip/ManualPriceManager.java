/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages manual price overrides set by the user.
 */
public class ManualPriceManager
{
private final Map<Integer, Integer> manualBuyPrices;
private final Map<Integer, Integer> manualSellPrices;

public ManualPriceManager()
{
this.manualBuyPrices = new ConcurrentHashMap<>();
this.manualSellPrices = new ConcurrentHashMap<>();
}

public void setManualBuyPrice(int itemId, int price)
{
if (price >= 0)
{
manualBuyPrices.put(itemId, price);
}
}

public void setManualSellPrice(int itemId, int price)
{
if (price >= 0)
{
manualSellPrices.put(itemId, price);
}
}

public Integer getManualBuyPrice(int itemId)
{
return manualBuyPrices.get(itemId);
}

public Integer getManualSellPrice(int itemId)
{
return manualSellPrices.get(itemId);
}

public int getEffectiveBuyPrice(int itemId, int gePrice)
{
Integer manual = getManualBuyPrice(itemId);
return manual != null ? manual : gePrice;
}

public int getEffectiveSellPrice(int itemId, int gePrice)
{
Integer manual = getManualSellPrice(itemId);
return manual != null ? manual : gePrice;
}

public void clearManualPrices(int itemId)
{
manualBuyPrices.remove(itemId);
manualSellPrices.remove(itemId);
}

public boolean hasManualPrice(int itemId)
{
return manualBuyPrices.containsKey(itemId) || manualSellPrices.containsKey(itemId);
}
}
