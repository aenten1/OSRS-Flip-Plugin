/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

@Slf4j
public class DataManager
{
	private static final String DIR_NAME = "osrs-flip-plugin";
	private static final String FILE_NAME = "data.json";

	private final Gson gson;
	private final File dataFile;

	public DataManager(Gson gson)
	{
		this.gson = gson.newBuilder().setPrettyPrinting().create();
		File dir = new File(RuneLite.RUNELITE_DIR, DIR_NAME);
		dir.mkdirs();
		this.dataFile = new File(dir, FILE_NAME);
	}

	public void save(List<Integer> watchlist, Map<Integer, Boolean> watchlistOffsets, ManualPriceManager manualPriceManager, CombinationRecipeManager recipeManager)
	{
		SaveData data = new SaveData();

		// Watchlist with manual prices and offset flags
		for (int itemId : watchlist)
		{
			WatchlistEntry entry = new WatchlistEntry();
			entry.itemId = itemId;
			entry.manualBuyPrice = manualPriceManager.getManualBuyPrice(itemId);
			entry.manualSellPrice = manualPriceManager.getManualSellPrice(itemId);
			entry.applyOffsets = watchlistOffsets.getOrDefault(itemId, false);
			data.watchlist.add(entry);
		}

		// Recipes
		for (CombinationRecipe recipe : recipeManager.getAllRecipes())
		{
			RecipeEntry entry = new RecipeEntry();
			entry.name = recipe.getName();
			entry.resultItemId = recipe.getResultItemId();
			entry.resultQuantity = recipe.getResultQuantity();
			entry.ingredients = new HashMap<>(recipe.getIngredients());
			entry.applyOffsets = recipe.isApplyOffsets();
			entry.collapsed = recipe.isCollapsed();
			data.recipes.add(entry);
		}

		try (FileWriter writer = new FileWriter(dataFile))
		{
			gson.toJson(data, writer);
			log.debug("Saved plugin data to {}", dataFile.getAbsolutePath());
		}
		catch (IOException e)
		{
			log.debug("Failed to save plugin data: {}", e.getMessage());
		}
	}

	public void load(List<Integer> watchlist, Map<Integer, Boolean> watchlistOffsets, ManualPriceManager manualPriceManager, CombinationRecipeManager recipeManager)
	{
		if (!dataFile.exists())
		{
			return;
		}

		try (FileReader reader = new FileReader(dataFile))
		{
			Type type = new TypeToken<SaveData>(){}.getType();
			SaveData data = gson.fromJson(reader, type);

			if (data == null)
			{
				return;
			}

			// Restore watchlist
			if (data.watchlist != null)
			{
				for (WatchlistEntry entry : data.watchlist)
				{
					if (!watchlist.contains(entry.itemId))
					{
						watchlist.add(entry.itemId);
					}
					if (entry.manualBuyPrice != null)
					{
						manualPriceManager.setManualBuyPrice(entry.itemId, entry.manualBuyPrice);
					}
					if (entry.manualSellPrice != null)
					{
						manualPriceManager.setManualSellPrice(entry.itemId, entry.manualSellPrice);
					}
					if (entry.applyOffsets)
					{
						watchlistOffsets.put(entry.itemId, true);
					}
				}
			}

			// Restore recipes
			if (data.recipes != null)
			{
				for (RecipeEntry entry : data.recipes)
				{
					int qty = entry.resultQuantity > 0 ? entry.resultQuantity : 1;
					CombinationRecipe recipe = new CombinationRecipe(entry.name, entry.resultItemId, qty);
					recipe.setApplyOffsets(entry.applyOffsets);
					recipe.setCollapsed(entry.collapsed);
					if (entry.ingredients != null)
					{
						for (Map.Entry<Integer, Integer> ing : entry.ingredients.entrySet())
						{
							recipe.addIngredient(ing.getKey(), ing.getValue());
						}
					}
					recipeManager.addRecipe(recipe);
				}
			}

			log.debug("Loaded plugin data from {}", dataFile.getAbsolutePath());
		}
		catch (Exception e)
		{
			log.debug("Failed to load plugin data: {}", e.getMessage());
		}
	}

	static class SaveData
	{
		List<WatchlistEntry> watchlist = new ArrayList<>();
		List<RecipeEntry> recipes = new ArrayList<>();
	}

	static class WatchlistEntry
	{
		int itemId;
		Integer manualBuyPrice;
		Integer manualSellPrice;
		boolean applyOffsets;
	}

	static class RecipeEntry
	{
		String name;
		int resultItemId;
		int resultQuantity = 1;
		Map<Integer, Integer> ingredients;
		boolean applyOffsets;
		boolean collapsed;
	}
}
