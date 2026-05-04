/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.game.ItemManager;

/**
 * Manages item prices from the Grand Exchange cache.
 */
@Slf4j
public class PriceManager
{
private final ItemManager itemManager;

public PriceManager(ItemManager itemManager)
{
this.itemManager = itemManager;
}

public int getBuyPrice(int itemId)
{
try
{
ItemComposition composition = itemManager.getItemComposition(itemId);
if (composition != null && composition.getPrice() > 0)
{
return composition.getPrice();
}
}
catch (Exception e)
{
log.debug("Failed to get buy price for item {}: {}", itemId, e.getMessage());
}
return -1;
}

public int getSellPrice(int itemId)
{
try
{
int gePrice = itemManager.getItemPrice(itemId);
if (gePrice > 0)
{
return gePrice;
}
}
catch (Exception e)
{
log.debug("Failed to get sell price for item {}: {}", itemId, e.getMessage());
}
return -1;
}

public int getLastSalePrice(int itemId)
{
return getSellPrice(itemId);
}

public boolean hasValidPrice(int itemId)
{
return getSellPrice(itemId) > 0 || getBuyPrice(itemId) > 0;
}

public String getItemName(int itemId)
{
try
{
ItemComposition composition = itemManager.getItemComposition(itemId);
if (composition != null)
{
return composition.getName();
}
}
catch (Exception e)
{
log.debug("Failed to get name for item {}: {}", itemId, e.getMessage());
}
return null;
}
}
