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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class OsrsFlipPanel extends PluginPanel
{
	private static final NumberFormat GP_FORMAT = NumberFormat.getInstance();

	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final ManualPriceManager manualPriceManager;
	private final ProfitCalculator profitCalculator;
	private final CombinationRecipeManager recipeManager;
	private final DataManager dataManager;
	private final OsrsFlipConfig config;

	@Getter
	private final List<Integer> watchlist = new ArrayList<>();
	private final Map<Integer, String> nameCache = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> priceCache = new ConcurrentHashMap<>();

	private JPanel watchlistPanel;
	private JPanel combinationsPanel;
	private JTextField searchField;

	public OsrsFlipPanel(
		ItemManager itemManager,
		ClientThread clientThread,
		ManualPriceManager manualPriceManager,
		ProfitCalculator profitCalculator,
		CombinationRecipeManager recipeManager,
		DataManager dataManager,
		OsrsFlipConfig config)
	{
		super(false);
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.manualPriceManager = manualPriceManager;
		this.profitCalculator = profitCalculator;
		this.recipeManager = recipeManager;
		this.dataManager = dataManager;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);
		add(buildTabs(), BorderLayout.CENTER);
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout(0, 5));
		header.setBorder(new EmptyBorder(5, 0, 5, 0));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Flip Tracker");
		title.setForeground(Color.WHITE);
		header.add(title, BorderLayout.NORTH);

		JPanel searchRow = new JPanel(new BorderLayout(5, 0));
		searchRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

		searchField = new JTextField();
		searchField.setToolTipText("Enter item ID to add to watchlist");
		searchField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					addItemFromSearch();
				}
			}
		});
		searchRow.add(searchField, BorderLayout.CENTER);

		JButton addButton = new JButton("Add");
		addButton.addActionListener(e -> addItemFromSearch());
		searchRow.add(addButton, BorderLayout.EAST);

		header.add(searchRow, BorderLayout.CENTER);

		JLabel hint = new JLabel("Enter item ID (e.g. 314 for Feather)");
		hint.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(hint, BorderLayout.SOUTH);

		return header;
	}

	private JTabbedPane buildTabs()
	{
		JTabbedPane tabs = new JTabbedPane();
		tabs.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Watchlist tab
		watchlistPanel = new JPanel();
		watchlistPanel.setLayout(new BoxLayout(watchlistPanel, BoxLayout.Y_AXIS));
		watchlistPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane watchScroll = new JScrollPane(watchlistPanel);
		watchScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		watchScroll.setBorder(null);

		// Combinations tab
		combinationsPanel = new JPanel();
		combinationsPanel.setLayout(new BoxLayout(combinationsPanel, BoxLayout.Y_AXIS));
		combinationsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel comboWrapper = new JPanel(new BorderLayout());
		comboWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton addRecipeBtn = new JButton("+ Add Recipe");
		addRecipeBtn.addActionListener(e -> showAddRecipeDialog(null));
		JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
		btnWrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
		btnWrap.add(addRecipeBtn);
		comboWrapper.add(btnWrap, BorderLayout.NORTH);

		JScrollPane comboScroll = new JScrollPane(combinationsPanel);
		comboScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		comboScroll.setBorder(null);
		comboWrapper.add(comboScroll, BorderLayout.CENTER);

		// Tab order based on config
		if (config.combinationsFirst())
		{
			tabs.addTab("Combinations", comboWrapper);
			tabs.addTab("Watchlist", watchScroll);
		}
		else
		{
			tabs.addTab("Watchlist", watchScroll);
			tabs.addTab("Combinations", comboWrapper);
		}

		return tabs;
	}

	private void addItemFromSearch()
	{
		String text = searchField.getText().trim();
		if (text.isEmpty())
		{
			return;
		}

		try
		{
			int itemId = Integer.parseInt(text);
			if (itemId <= 0)
			{
				return;
			}

			if (!watchlist.contains(itemId))
			{
				watchlist.add(itemId);
				saveData();
				refreshWatchlist();
			}
			searchField.setText("");
		}
		catch (NumberFormatException e)
		{
			log.debug("Invalid item ID entered: {}", text);
		}
	}

	private void saveData()
	{
		dataManager.save(watchlist, manualPriceManager, recipeManager);
	}

	/**
	 * Fetches item data on the client thread, then rebuilds the UI on the EDT.
	 */
	public void refreshWatchlist()
	{
		clientThread.invokeLater(() ->
		{
			for (int itemId : watchlist)
			{
				cacheItemData(itemId);
			}
			SwingUtilities.invokeLater(this::rebuildWatchlistUi);
		});
	}

	/**
	 * Fetches recipe item data on the client thread, then rebuilds the UI on the EDT.
	 */
	public void refreshCombinations()
	{
		clientThread.invokeLater(() ->
		{
			for (CombinationRecipe recipe : recipeManager.getAllRecipes())
			{
				cacheItemData(recipe.getResultItemId());
				for (int ingId : recipe.getIngredients().keySet())
				{
					cacheItemData(ingId);
				}
			}
			SwingUtilities.invokeLater(this::rebuildCombinationsUi);
		});
	}

	private void cacheItemData(int itemId)
	{
		try
		{
			String name = itemManager.getItemComposition(itemId).getName();
			if (name != null && !name.equals("null"))
			{
				nameCache.put(itemId, name);
			}
		}
		catch (Exception e)
		{
			log.debug("Failed to get name for item {}", itemId);
		}

		try
		{
			int price = itemManager.getItemPrice(itemId);
			priceCache.put(itemId, price);
		}
		catch (Exception e)
		{
			log.debug("Failed to get price for item {}", itemId);
		}
	}

	private String getCachedName(int itemId)
	{
		return nameCache.getOrDefault(itemId, "Item #" + itemId);
	}

	private int getCachedPrice(int itemId)
	{
		return priceCache.getOrDefault(itemId, -1);
	}

	private void rebuildWatchlistUi()
	{
		watchlistPanel.removeAll();

		if (watchlist.isEmpty())
		{
			JLabel empty = new JLabel("No items in watchlist. Add an item ID above.");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setBorder(new EmptyBorder(10, 5, 10, 5));
			watchlistPanel.add(empty);
		}
		else
		{
			for (int itemId : watchlist)
			{
				watchlistPanel.add(buildItemRow(itemId));
			}
		}

		watchlistPanel.revalidate();
		watchlistPanel.repaint();

		if (watchlistPanel.getParent() != null)
		{
			watchlistPanel.getParent().revalidate();
			watchlistPanel.getParent().repaint();
		}
	}

	private JPanel buildItemRow(int itemId)
	{
		JPanel row = new JPanel(new BorderLayout(5, 2));
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 5, 8, 5)
		));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

		String name = getCachedName(itemId);

		// Header: item name + buttons
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(name);
		nameLabel.setForeground(Color.WHITE);
		header.add(nameLabel, BorderLayout.WEST);

		JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		headerButtons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JButton overrideBtn = new JButton("Edit");
		overrideBtn.setToolTipText("Set manual prices");
		overrideBtn.addActionListener(e -> showOverrideDialog(itemId));
		headerButtons.add(overrideBtn);

		JButton removeBtn = new JButton("X");
		removeBtn.setToolTipText("Remove from watchlist");
		removeBtn.addActionListener(e ->
		{
			watchlist.remove(Integer.valueOf(itemId));
			manualPriceManager.clearManualPrices(itemId);
			saveData();
			refreshWatchlist();
		});
		headerButtons.add(removeBtn);

		header.add(headerButtons, BorderLayout.EAST);
		row.add(header, BorderLayout.NORTH);

		// Price info
		int gePrice = getCachedPrice(itemId);

		int effectiveBuy = manualPriceManager.getEffectiveBuyPrice(itemId, gePrice);
		int effectiveSell = manualPriceManager.getEffectiveSellPrice(itemId, gePrice);

		JPanel priceGrid = new JPanel(new GridLayout(3, 2, 4, 2));
		priceGrid.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		boolean hasManualBuy = manualPriceManager.getManualBuyPrice(itemId) != null;
		boolean hasManualSell = manualPriceManager.getManualSellPrice(itemId) != null;

		priceGrid.add(makeLabel("Buy:", ColorScheme.LIGHT_GRAY_COLOR));
		priceGrid.add(makeLabel(
			formatGp(effectiveBuy) + (hasManualBuy ? " *" : ""),
			effectiveBuy > 0 ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR));

		priceGrid.add(makeLabel("Sell:", ColorScheme.LIGHT_GRAY_COLOR));
		priceGrid.add(makeLabel(
			formatGp(effectiveSell) + (hasManualSell ? " *" : ""),
			effectiveSell > 0 ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR));

		int profit = profitCalculator.calculateBasicProfit(effectiveBuy, effectiveSell);
		Color profitColor = profit >= 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.RED;

		priceGrid.add(makeLabel("Profit:", ColorScheme.LIGHT_GRAY_COLOR));
		priceGrid.add(makeLabel(formatGp(profit), profitColor));

		row.add(priceGrid, BorderLayout.CENTER);

		return row;
	}

	private void showOverrideDialog(int itemId)
	{
		String name = getCachedName(itemId);

		JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
		JTextField buyField = new JTextField();
		JTextField sellField = new JTextField();

		Integer manualBuy = manualPriceManager.getManualBuyPrice(itemId);
		Integer manualSell = manualPriceManager.getManualSellPrice(itemId);
		if (manualBuy != null) buyField.setText(String.valueOf(manualBuy));
		if (manualSell != null) sellField.setText(String.valueOf(manualSell));

		panel.add(new JLabel("Buy Price:"));
		panel.add(buyField);
		panel.add(new JLabel("Sell Price:"));
		panel.add(sellField);

		int result = JOptionPane.showConfirmDialog(this, panel,
			"Manual Prices - " + name, JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION)
		{
			try
			{
				String buyText = buyField.getText().trim();
				String sellText = sellField.getText().trim();

				if (buyText.isEmpty() && sellText.isEmpty())
				{
					manualPriceManager.clearManualPrices(itemId);
				}
				else
				{
					if (!buyText.isEmpty())
					{
						manualPriceManager.setManualBuyPrice(itemId, Integer.parseInt(buyText));
					}
					if (!sellText.isEmpty())
					{
						manualPriceManager.setManualSellPrice(itemId, Integer.parseInt(sellText));
					}
				}
				saveData();
				refreshWatchlist();
			}
			catch (NumberFormatException e)
			{
				log.debug("Invalid price entered");
			}
		}
	}

	/**
	 * Shows the add/edit recipe dialog. If editingRecipe is non-null, pre-fills the form.
	 */
	private void showAddRecipeDialog(CombinationRecipe editingRecipe)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 10));

		// Top section: recipe name and result
		JPanel topSection = new JPanel(new GridLayout(2, 2, 5, 5));
		JTextField nameField = new JTextField();
		JTextField resultField = new JTextField();

		if (editingRecipe != null)
		{
			nameField.setText(editingRecipe.getName());
			resultField.setText(String.valueOf(editingRecipe.getResultItemId()));
		}

		topSection.add(new JLabel("Recipe Name:"));
		topSection.add(nameField);
		topSection.add(new JLabel("Result Item ID:"));
		topSection.add(resultField);
		panel.add(topSection, BorderLayout.NORTH);

		// Ingredient rows
		JPanel ingredientsContainer = new JPanel();
		ingredientsContainer.setLayout(new BoxLayout(ingredientsContainer, BoxLayout.Y_AXIS));

		List<JTextField[]> ingredientRows = new ArrayList<>();

		Runnable addIngredientRow = () ->
		{
			JPanel row = new JPanel(new GridLayout(1, 4, 5, 0));
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
			JTextField idField = new JTextField();
			JTextField qtyField = new JTextField();
			row.add(new JLabel("  Item ID:"));
			row.add(idField);
			row.add(new JLabel("  Qty:"));
			row.add(qtyField);
			ingredientRows.add(new JTextField[]{idField, qtyField});
			ingredientsContainer.add(row);
			ingredientsContainer.revalidate();
			ingredientsContainer.repaint();
		};

		// Pre-fill ingredient rows if editing
		if (editingRecipe != null && !editingRecipe.getIngredients().isEmpty())
		{
			for (Map.Entry<Integer, Integer> entry : editingRecipe.getIngredients().entrySet())
			{
				JPanel row = new JPanel(new GridLayout(1, 4, 5, 0));
				row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
				JTextField idField = new JTextField(String.valueOf(entry.getKey()));
				JTextField qtyField = new JTextField(String.valueOf(entry.getValue()));
				row.add(new JLabel("  Item ID:"));
				row.add(idField);
				row.add(new JLabel("  Qty:"));
				row.add(qtyField);
				ingredientRows.add(new JTextField[]{idField, qtyField});
				ingredientsContainer.add(row);
			}
		}
		else
		{
			addIngredientRow.run();
		}

		JPanel ingredientSection = new JPanel(new BorderLayout(0, 5));
		JPanel labelRow = new JPanel(new BorderLayout());
		labelRow.add(new JLabel("Ingredients:"), BorderLayout.WEST);
		JButton addMoreBtn = new JButton("+ Add Ingredient");
		addMoreBtn.addActionListener(e -> addIngredientRow.run());
		labelRow.add(addMoreBtn, BorderLayout.EAST);
		ingredientSection.add(labelRow, BorderLayout.NORTH);

		JScrollPane ingredientScroll = new JScrollPane(ingredientsContainer);
		ingredientScroll.setPreferredSize(new Dimension(400, 120));
		ingredientSection.add(ingredientScroll, BorderLayout.CENTER);

		panel.add(ingredientSection, BorderLayout.CENTER);

		String dialogTitle = editingRecipe != null ? "Edit Recipe" : "Add Combination Recipe";
		int result = JOptionPane.showConfirmDialog(this, panel,
			dialogTitle, JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION)
		{
			try
			{
				String rName = nameField.getText().trim();
				int resultId = Integer.parseInt(resultField.getText().trim());

				if (rName.isEmpty())
				{
					return;
				}

				// Remove old recipe if editing
				if (editingRecipe != null)
				{
					recipeManager.removeRecipe(editingRecipe.getName());
				}

				CombinationRecipe recipe = new CombinationRecipe(rName, resultId);

				for (JTextField[] fields : ingredientRows)
				{
					String idText = fields[0].getText().trim();
					String qtyText = fields[1].getText().trim();
					if (!idText.isEmpty() && !qtyText.isEmpty())
					{
						recipe.addIngredient(Integer.parseInt(idText), Integer.parseInt(qtyText));
					}
				}

				if (!recipe.getIngredients().isEmpty())
				{
					recipeManager.addRecipe(recipe);
					saveData();
					refreshCombinations();
				}
			}
			catch (NumberFormatException e)
			{
				log.debug("Invalid recipe input");
			}
		}
	}

	private void rebuildCombinationsUi()
	{
		combinationsPanel.removeAll();

		List<CombinationRecipe> recipes = recipeManager.getAllRecipes();

		if (recipes.isEmpty())
		{
			JLabel empty = new JLabel("No recipes. Click '+ Add Recipe' above.");
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setBorder(new EmptyBorder(10, 5, 10, 5));
			combinationsPanel.add(empty);
		}
		else
		{
			for (CombinationRecipe recipe : recipes)
			{
				combinationsPanel.add(buildRecipeRow(recipe));
			}
		}

		combinationsPanel.revalidate();
		combinationsPanel.repaint();

		if (combinationsPanel.getParent() != null)
		{
			combinationsPanel.getParent().revalidate();
			combinationsPanel.getParent().repaint();
		}
	}

	private JPanel buildRecipeRow(CombinationRecipe recipe)
	{
		JPanel row = new JPanel(new BorderLayout(0, 0));
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(4, 5, 4, 5)
		));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Top bar: recipe name + Edit / X buttons — always visible
		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		topBar.setBorder(new EmptyBorder(0, 0, 4, 0));

		JLabel nameLabel = new JLabel(recipe.getName());
		nameLabel.setForeground(Color.WHITE);
		topBar.add(nameLabel, BorderLayout.WEST);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JButton editBtn = new JButton("Edit");
		editBtn.addActionListener(e -> showAddRecipeDialog(recipe));
		buttons.add(editBtn);

		JButton removeBtn = new JButton("X");
		removeBtn.addActionListener(e ->
		{
			recipeManager.removeRecipe(recipe.getName());
			saveData();
			refreshCombinations();
		});
		buttons.add(removeBtn);

		topBar.add(buttons, BorderLayout.EAST);
		row.add(topBar, BorderLayout.NORTH);

		// Body: compact multi-line breakdown
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// Ingredients
		int totalIngredientCost = 0;
		for (Map.Entry<Integer, Integer> entry : recipe.getIngredients().entrySet())
		{
			int ingId = entry.getKey();
			int qty = entry.getValue();
			String ingName = getCachedName(ingId);

			int unitPrice = getCachedPrice(ingId);
			int lineCost = unitPrice > 0 ? unitPrice * qty : 0;
			totalIngredientCost += lineCost;

			addLine(body, GP_FORMAT.format(qty) + "x " + ingName, ColorScheme.LIGHT_GRAY_COLOR);
			addLine(body, "  @ " + formatGp(unitPrice) + " = " + formatGp(lineCost), ColorScheme.LIGHT_GRAY_COLOR);
		}

		// Separator
		addLine(body, "---", ColorScheme.MEDIUM_GRAY_COLOR);

		// Profit breakdown — each value on its own line
		String resultName = getCachedName(recipe.getResultItemId());
		int resultPrice = getCachedPrice(recipe.getResultItemId());
		int tax = resultPrice > 0 ? profitCalculator.calculateTax(resultPrice) : 0;
		int profit = profitCalculator.calculateCombinationProfit(recipe, totalIngredientCost, resultPrice);
		ProfitThreshold threshold = profitCalculator.getProfitThreshold(profit);

		addLine(body, "Sell: " + formatGp(resultPrice), Color.WHITE);
		addLine(body, "GE Tax: -" + formatGp(tax), new Color(255, 180, 100));
		addLine(body, "Cost: -" + formatGp(totalIngredientCost), ColorScheme.LIGHT_GRAY_COLOR);
		Color profitColor = profit >= 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.RED;
		addLine(body, "Profit: " + formatGp(profit), profitColor);

		row.add(body, BorderLayout.CENTER);

		return row;
	}

	private void addLine(JPanel container, String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setForeground(color);
		container.add(label);
	}

	private JLabel makeLabel(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setForeground(color);
		return label;
	}

	private String formatGp(int amount)
	{
		if (amount < 0)
		{
			return "N/A";
		}
		return GP_FORMAT.format(amount) + " gp";
	}

	private Color getProfitColor(ProfitThreshold threshold)
	{
		switch (threshold)
		{
			case LEGENDARY:
			case ULTRA_HIGH:
				return new Color(255, 215, 0); // gold
			case VERY_HIGH:
			case HIGH:
				return ColorScheme.PROGRESS_COMPLETE_COLOR; // green
			case MEDIUM:
				return Color.YELLOW;
			case LOW:
				return Color.WHITE;
			case NONE:
			default:
				return Color.RED;
		}
	}
}
