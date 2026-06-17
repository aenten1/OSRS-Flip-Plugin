/*
 * Copyright (c) 2024, OSRS Flip Plugin Developers
 * All rights reserved.
 */
package net.runelite.client.plugins.osrsflip;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a combination recipe for crafting items.
 */
@Getter
public class CombinationRecipe
{
	private final String name;
	private final int resultItemId;
	private final int resultQuantity;
	private final Map<Integer, Integer> ingredients;
	@Setter
	private boolean applyOffsets;
	@Setter
	private boolean collapsed;

	public CombinationRecipe(String name, int resultItemId, int resultQuantity)
	{
		this.name = name;
		this.resultItemId = resultItemId;
		this.resultQuantity = Math.max(1, resultQuantity);
		this.ingredients = new HashMap<>();
	}

	public void addIngredient(int itemId, int quantity)
	{
		ingredients.put(itemId, quantity);
	}

	public int getIngredientQuantity(int itemId)
	{
		return ingredients.getOrDefault(itemId, 0);
	}

	public boolean validate()
	{
		if (ingredients.size() > 10)
		{
			return false;
		}

		for (int quantity : ingredients.values())
		{
			if (quantity <= 0)
			{
				return false;
			}
		}

		return true;
	}

	public int getTotalIngredientCount()
	{
		return ingredients.values().stream().mapToInt(Integer::intValue).sum();
	}
}
