/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;

/**
 * Utility for searching items by name or ID.
 */
@Slf4j
public class ItemSearcher
{
private Client client;

public ItemSearcher(Client client)
{
this.client = client;
}

public String getNameById(int itemId)
{
if (itemId <= 0 || client == null)
{
return null;
}

try
{
ItemComposition composition = client.getItemDefinition(itemId);
if (composition != null && !composition.getName().equals("null"))
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

public List<Integer> searchByName(String search)
{
List<Integer> results = new ArrayList<>();

if (search == null || search.trim().isEmpty() || client == null)
{
return results;
}

String searchTerm = search.toLowerCase().trim();

try
{
for (int i = 0; i < client.getItemCount(); i++)
{
ItemComposition comp = client.getItemDefinition(i);
if (comp != null && comp.getName() != null && !comp.getName().equals("null"))
{
if (comp.getName().toLowerCase().contains(searchTerm))
{
results.add(i);
}
}
}
}
catch (Exception e)
{
log.debug("Failed to search for items matching '{}': {}", search, e.getMessage());
}

return results;
}

public void refreshCaches()
{
log.debug("ItemSearcher cache refresh requested");
}

public void clearCaches()
{
log.debug("ItemSearcher cache cleared");
}
}
