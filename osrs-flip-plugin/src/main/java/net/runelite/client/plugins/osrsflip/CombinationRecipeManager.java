/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Manages combination recipes for crafting items.
 */
public class CombinationRecipeManager
{
private final Map<String, CombinationRecipe> recipes;

public CombinationRecipeManager()
{
this.recipes = new ConcurrentHashMap<>();
}

public void addRecipe(CombinationRecipe recipe)
{
if (recipe.validate())
{
recipes.put(recipe.getName(), recipe);
}
}

public CombinationRecipe getRecipe(String name)
{
return recipes.get(name);
}

public List<CombinationRecipe> getAllRecipes()
{
return new ArrayList<>(recipes.values());
}

public boolean removeRecipe(String name)
{
return recipes.remove(name) != null;
}

public int getRecipeCount()
{
return recipes.size();
}
}
