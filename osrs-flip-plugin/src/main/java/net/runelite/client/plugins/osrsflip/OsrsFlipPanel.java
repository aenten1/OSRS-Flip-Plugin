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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class OsrsFlipPanel extends PluginPanel
{
	private static final NumberFormat GP_FORMAT = NumberFormat.getInstance();
	private static final int SEARCH_DELAY_MS = 300;
	private static final int MAX_SEARCH_RESULTS = 10;

	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final ManualPriceManager manualPriceManager;
	private final ProfitCalculator profitCalculator;
	private final CombinationRecipeManager recipeManager;
	private final DataManager dataManager;
	private final WikiPriceManager wikiPriceManager;
	private final OsrsFlipConfig config;

	@Getter
	private final List<Integer> watchlist = new ArrayList<>();
	private final Map<Integer, String> nameCache = new ConcurrentHashMap<>();
	private final Map<Integer, int[]> priceCache = new ConcurrentHashMap<>();

	private JPanel watchlistPanel;
	private JPanel combinationsPanel;
	private JTextField searchField;
	private JLabel searchStatus;
	private Timer searchTimer;

	public OsrsFlipPanel(
		ItemManager itemManager,
		ClientThread clientThread,
		ManualPriceManager manualPriceManager,
		ProfitCalculator profitCalculator,
		CombinationRecipeManager recipeManager,
		DataManager dataManager,
		WikiPriceManager wikiPriceManager,
		OsrsFlipConfig config)
	{
		super(false);
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.manualPriceManager = manualPriceManager;
		this.profitCalculator = profitCalculator;
		this.recipeManager = recipeManager;
		this.dataManager = dataManager;
		this.wikiPriceManager = wikiPriceManager;
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
		searchField.setToolTipText("Search by name or item ID");

		// Debounced search on keystrokes
		searchTimer = new Timer(SEARCH_DELAY_MS, e -> triggerSearch());
		searchTimer.setRepeats(false);

		searchField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					searchTimer.stop();
					addItemFromSearch();
				}
				else
				{
					searchTimer.restart();
				}
			}
		});
		searchRow.add(searchField, BorderLayout.CENTER);

		JButton addButton = new JButton("Add");
		addButton.addActionListener(e -> addItemFromSearch());
		searchRow.add(addButton, BorderLayout.EAST);

		header.add(searchRow, BorderLayout.CENTER);

		searchStatus = new JLabel("Search by name or item ID");
		searchStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(searchStatus, BorderLayout.SOUTH);

		return header;
	}

	/**
	 * Triggers a name search on the client thread and shows a dropdown.
	 */
	private void triggerSearch()
	{
		String text = searchField.getText().trim();
		if (text.isEmpty())
		{
			searchStatus.setText("Search by name or item ID");
			searchStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			return;
		}

		// If numeric, resolve the name
		try
		{
			int itemId = Integer.parseInt(text);
			if (itemId > 0)
			{
				clientThread.invokeLater(() ->
				{
					String name = resolveItemName(itemId);
					SwingUtilities.invokeLater(() ->
					{
						if (name != null)
						{
							searchStatus.setText(name + " (#" + itemId + ")");
							searchStatus.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
						}
						else
						{
							searchStatus.setText("Unknown item ID: " + itemId);
							searchStatus.setForeground(Color.RED);
						}
					});
				});
			}
			return;
		}
		catch (NumberFormatException ignored)
		{
		}

		// Name search
		searchStatus.setText("Searching...");
		searchStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		clientThread.invokeLater(() ->
		{
			List<int[]> results = searchItemsByName(text);
			SwingUtilities.invokeLater(() -> showSearchResults(results));
		});
	}

	/**
	 * Searches items by name on the client thread. Returns list of [itemId].
	 * Item names are cached in nameCache as a side effect.
	 */
	private List<int[]> searchItemsByName(String query)
	{
		List<int[]> results = new ArrayList<>();
		String searchTerm = query.toLowerCase().trim();

		try
		{
			int count = itemManager.getItemComposition(0) != null ? 30000 : 0;
			for (int i = 0; i < count && results.size() < MAX_SEARCH_RESULTS; i++)
			{
				try
				{
					ItemComposition comp = itemManager.getItemComposition(i);
					if (comp != null && comp.getName() != null
						&& !comp.getName().equals("null")
						&& comp.getName().toLowerCase().contains(searchTerm))
					{
						nameCache.put(i, comp.getName());
						results.add(new int[]{i});
					}
				}
				catch (Exception e)
				{
					// skip invalid items
				}
			}
		}
		catch (Exception e)
		{
			log.debug("Search failed: {}", e.getMessage());
		}

		return results;
	}

	private void showSearchResults(List<int[]> results)
	{
		if (results.isEmpty())
		{
			searchStatus.setText("No items found");
			searchStatus.setForeground(Color.RED);
			return;
		}

		searchStatus.setText(results.size() + " result(s) — click to add");
		searchStatus.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPopupMenu popup = new JPopupMenu();
		for (int[] result : results)
		{
			int itemId = result[0];
			String name = nameCache.getOrDefault(itemId, "Item #" + itemId);
			JMenuItem item = new JMenuItem(name + "  (#" + itemId + ")");
			item.addActionListener(e ->
			{
				if (!watchlist.contains(itemId))
				{
					watchlist.add(itemId);
					saveData();
					refreshWatchlist();
				}
				searchField.setText("");
				searchStatus.setText("Added: " + name);
				searchStatus.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			});
			popup.add(item);
		}

		popup.show(searchField, 0, searchField.getHeight());
	}

	private void addItemFromSearch()
	{
		String text = searchField.getText().trim();
		if (text.isEmpty())
		{
			return;
		}

		// Try numeric ID first
		try
		{
			int itemId = Integer.parseInt(text);
			if (itemId <= 0)
			{
				searchStatus.setText("Item ID must be positive");
				searchStatus.setForeground(Color.RED);
				return;
			}

			// Validate the ID exists
			clientThread.invokeLater(() ->
			{
				String name = resolveItemName(itemId);
				SwingUtilities.invokeLater(() ->
				{
					if (name == null)
					{
						searchStatus.setText("Unknown item ID: " + itemId);
						searchStatus.setForeground(Color.RED);
						return;
					}

					if (!watchlist.contains(itemId))
					{
						watchlist.add(itemId);
						saveData();
						refreshWatchlist();
						searchField.setText("");
						searchStatus.setText("Added: " + name);
						searchStatus.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
					}
					else
					{
						searchField.setText("");
						searchStatus.setText("Already in watchlist: " + name);
						searchStatus.setForeground(Color.YELLOW);
					}
				});
			});
			return;
		}
		catch (NumberFormatException ignored)
		{
		}

		// Non-numeric: trigger a search
		triggerSearch();
	}

	/**
	 * Resolves an item ID to a name. Must be called on the client thread.
	 * Returns null if the ID is invalid.
	 */
	private String resolveItemName(int itemId)
	{
		try
		{
			ItemComposition comp = itemManager.getItemComposition(itemId);
			if (comp != null && comp.getName() != null && !comp.getName().equals("null"))
			{
				nameCache.put(itemId, comp.getName());
				return comp.getName();
			}
		}
		catch (Exception e)
		{
			log.debug("Failed to resolve item {}", itemId);
		}
		return null;
	}

	/**
	 * Adds a name-preview label to an ID text field. When a valid ID is typed,
	 * the label shows the item name. Supports name search with dropdown.
	 */
	private JLabel attachNamePreview(JTextField idField, JPanel parentForPopup)
	{
		JLabel preview = new JLabel(" ");
		preview.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		Timer timer = new Timer(SEARCH_DELAY_MS, e ->
		{
			String text = idField.getText().trim();
			if (text.isEmpty())
			{
				preview.setText(" ");
				preview.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				return;
			}

			// Numeric: resolve name
			try
			{
				int itemId = Integer.parseInt(text);
				if (itemId > 0)
				{
					clientThread.invokeLater(() ->
					{
						String name = resolveItemName(itemId);
						SwingUtilities.invokeLater(() ->
						{
							if (name != null)
							{
								preview.setText(name);
								preview.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
							}
							else
							{
								preview.setText("Unknown ID");
								preview.setForeground(Color.RED);
							}
						});
					});
				}
				return;
			}
			catch (NumberFormatException ignored)
			{
			}

			// Name search
			preview.setText("Searching...");
			clientThread.invokeLater(() ->
			{
				List<int[]> results = searchItemsByName(text);
				SwingUtilities.invokeLater(() ->
				{
					if (results.isEmpty())
					{
						preview.setText("No items found");
						preview.setForeground(Color.RED);
					}
					else
					{
						JPopupMenu popup = new JPopupMenu();
						for (int[] result : results)
						{
							int id = result[0];
							String name = nameCache.getOrDefault(id, "Item #" + id);
							JMenuItem menuItem = new JMenuItem(name + "  (#" + id + ")");
							menuItem.addActionListener(ev ->
							{
								idField.setText(String.valueOf(id));
								preview.setText(name);
								preview.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
							});
							popup.add(menuItem);
						}
						preview.setText(results.size() + " result(s)");
						preview.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
						popup.show(idField, 0, idField.getHeight());
					}
				});
			});
		});
		timer.setRepeats(false);

		idField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				timer.restart();
			}
		});

		return preview;
	}

	private void saveData()
	{
		dataManager.save(watchlist, manualPriceManager, recipeManager);
	}

	public void refreshWatchlist()
	{
		List<Integer> ids = new ArrayList<>(watchlist);
		fetchPricesAndNames(ids, this::rebuildWatchlistUi);
	}

	public void refreshCombinations()
	{
		List<CombinationRecipe> recipes = recipeManager.getAllRecipes();
		log.info("refreshCombinations: {} recipes", recipes.size());

		List<Integer> ids = new ArrayList<>();
		for (CombinationRecipe recipe : recipes)
		{
			ids.add(recipe.getResultItemId());
			ids.addAll(recipe.getIngredients().keySet());
		}
		fetchPricesAndNames(ids, this::rebuildCombinationsUi);
	}

	private void fetchPricesAndNames(List<Integer> itemIds, Runnable uiCallback)
	{
		log.info("fetchPricesAndNames: {} items, wikiPrices={}", itemIds.size(), config.useWikiPrices());

		if (itemIds.isEmpty())
		{
			SwingUtilities.invokeLater(uiCallback);
			return;
		}

		if (config.useWikiPrices())
		{
			wikiPriceManager.fetchPrices(itemIds, () ->
			{
				// Cache wiki prices (safe on any thread — just reading from maps)
				for (int itemId : itemIds)
				{
					int buy = wikiPriceManager.getBuyPrice(itemId, config);
					int sell = wikiPriceManager.getSellPrice(itemId, config);
					priceCache.put(itemId, new int[]{buy, sell});
				}

				// Cache names + ItemManager fallback on client thread, then update UI
				clientThread.invokeLater(() ->
				{
					for (int itemId : itemIds)
					{
						cacheItemName(itemId);

						// Fallback to ItemManager if wiki returned nothing
						int[] cached = priceCache.get(itemId);
						if (cached != null && cached[0] < 0 && cached[1] < 0)
						{
							int fallback = itemManager.getItemPrice(itemId);
							if (fallback > 0)
							{
								priceCache.put(itemId, new int[]{fallback, fallback});
							}
						}
					}
					SwingUtilities.invokeLater(uiCallback);
				});
			});
		}
		else
		{
			clientThread.invokeLater(() ->
			{
				for (int itemId : itemIds)
				{
					cacheItemName(itemId);
					int price = itemManager.getItemPrice(itemId);
					priceCache.put(itemId, new int[]{price, price});
				}
				SwingUtilities.invokeLater(uiCallback);
			});
		}
	}

	private void cacheItemName(int itemId)
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
	}

	private String getCachedName(int itemId)
	{
		return nameCache.getOrDefault(itemId, "Item #" + itemId);
	}

	private int getCachedBuyPrice(int itemId)
	{
		int[] prices = priceCache.get(itemId);
		return prices != null ? prices[0] : -1;
	}

	private int getCachedSellPrice(int itemId)
	{
		int[] prices = priceCache.get(itemId);
		return prices != null ? prices[1] : -1;
	}

	private JTabbedPane buildTabs()
	{
		JTabbedPane tabs = new JTabbedPane();
		tabs.setBackground(ColorScheme.DARK_GRAY_COLOR);

		watchlistPanel = new JPanel();
		watchlistPanel.setLayout(new BoxLayout(watchlistPanel, BoxLayout.Y_AXIS));
		watchlistPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane watchScroll = new JScrollPane(watchlistPanel);
		watchScroll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		watchScroll.setBorder(null);

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

	private void rebuildWatchlistUi()
	{
		watchlistPanel.removeAll();

		if (watchlist.isEmpty())
		{
			JLabel empty = new JLabel("No items — search above to add.");
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

		JButton refreshBtn = new JButton("\u21BB");
		refreshBtn.setToolTipText("Refresh prices");
		refreshBtn.addActionListener(e -> refreshWatchlist());
		headerButtons.add(refreshBtn);

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

		int geBuy = getCachedBuyPrice(itemId);
		int geSell = getCachedSellPrice(itemId);

		int effectiveBuy = manualPriceManager.getEffectiveBuyPrice(itemId, geBuy);
		int effectiveSell = manualPriceManager.getEffectiveSellPrice(itemId, geSell);

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
				JOptionPane.showMessageDialog(this, "Prices must be valid numbers.",
					"Invalid Input", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void showAddRecipeDialog(CombinationRecipe editingRecipe)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 10));

		// Top section: recipe name, result item with name preview
		JPanel topSection = new JPanel();
		topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));

		JPanel nameRow = new JPanel(new BorderLayout(5, 0));
		nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		nameRow.add(new JLabel("Recipe Name:"), BorderLayout.WEST);
		JTextField nameField = new JTextField();
		nameRow.add(nameField, BorderLayout.CENTER);
		topSection.add(nameRow);

		JPanel resultRow = new JPanel(new BorderLayout(5, 0));
		resultRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		resultRow.add(new JLabel("Result Item:"), BorderLayout.WEST);
		JTextField resultField = new JTextField();
		resultRow.add(resultField, BorderLayout.CENTER);
		topSection.add(resultRow);

		JLabel resultPreview = attachNamePreview(resultField, panel);
		resultPreview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		topSection.add(resultPreview);

		if (editingRecipe != null)
		{
			nameField.setText(editingRecipe.getName());
			resultField.setText(String.valueOf(editingRecipe.getResultItemId()));
		}

		panel.add(topSection, BorderLayout.NORTH);

		// Ingredient rows with name previews
		JPanel ingredientsContainer = new JPanel();
		ingredientsContainer.setLayout(new BoxLayout(ingredientsContainer, BoxLayout.Y_AXIS));

		// Each entry: [idField, qtyField, previewLabel]
		List<Object[]> ingredientRows = new ArrayList<>();

		Runnable addIngredientRow = () ->
		{
			JPanel row = new JPanel(new BorderLayout(5, 0));
			row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

			JPanel fields = new JPanel(new GridLayout(1, 4, 5, 0));
			JTextField idField = new JTextField();
			JTextField qtyField = new JTextField();
			fields.add(new JLabel("  Item:"));
			fields.add(idField);
			fields.add(new JLabel("  Qty:"));
			fields.add(qtyField);
			row.add(fields, BorderLayout.NORTH);

			JLabel preview = attachNamePreview(idField, panel);
			row.add(preview, BorderLayout.SOUTH);

			ingredientRows.add(new Object[]{idField, qtyField, preview});
			ingredientsContainer.add(row);
			ingredientsContainer.revalidate();
			ingredientsContainer.repaint();
		};

		if (editingRecipe != null && !editingRecipe.getIngredients().isEmpty())
		{
			for (Map.Entry<Integer, Integer> entry : editingRecipe.getIngredients().entrySet())
			{
				JPanel row = new JPanel(new BorderLayout(5, 0));
				row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

				JPanel fields = new JPanel(new GridLayout(1, 4, 5, 0));
				JTextField idField = new JTextField(String.valueOf(entry.getKey()));
				JTextField qtyField = new JTextField(String.valueOf(entry.getValue()));
				fields.add(new JLabel("  Item:"));
				fields.add(idField);
				fields.add(new JLabel("  Qty:"));
				fields.add(qtyField);
				row.add(fields, BorderLayout.NORTH);

				JLabel preview = attachNamePreview(idField, panel);
				row.add(preview, BorderLayout.SOUTH);

				ingredientRows.add(new Object[]{idField, qtyField, preview});
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
		ingredientScroll.setPreferredSize(new Dimension(400, 150));
		ingredientSection.add(ingredientScroll, BorderLayout.CENTER);

		panel.add(ingredientSection, BorderLayout.CENTER);

		String dialogTitle = editingRecipe != null ? "Edit Recipe" : "Add Combination Recipe";
		int result = JOptionPane.showConfirmDialog(this, panel,
			dialogTitle, JOptionPane.OK_CANCEL_OPTION);

		if (result == JOptionPane.OK_OPTION)
		{
			// Validate inputs
			String rName = nameField.getText().trim();
			if (rName.isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Recipe name is required.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			String resultText = resultField.getText().trim();
			int resultId;
			try
			{
				resultId = Integer.parseInt(resultText);
				if (resultId <= 0)
				{
					JOptionPane.showMessageDialog(this, "Result item ID must be a positive number.",
						"Validation Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			catch (NumberFormatException e)
			{
				JOptionPane.showMessageDialog(this, "Result item ID must be a valid number: \"" + resultText + "\"",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (editingRecipe != null)
			{
				recipeManager.removeRecipe(editingRecipe.getName());
			}

			CombinationRecipe recipe = new CombinationRecipe(rName, resultId);
			int rowNum = 0;

			for (Object[] fields : ingredientRows)
			{
				rowNum++;
				JTextField idField = (JTextField) fields[0];
				JTextField qtyField = (JTextField) fields[1];

				String idText = idField.getText().trim();
				String qtyText = qtyField.getText().trim();

				if (idText.isEmpty() && qtyText.isEmpty())
				{
					continue;
				}

				int ingId;
				try
				{
					ingId = Integer.parseInt(idText);
					if (ingId <= 0)
					{
						JOptionPane.showMessageDialog(this,
							"Ingredient row " + rowNum + ": Item ID must be a positive number.",
							"Validation Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				catch (NumberFormatException e)
				{
					JOptionPane.showMessageDialog(this,
						"Ingredient row " + rowNum + ": Invalid item ID \"" + idText + "\"",
						"Validation Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				int qty;
				try
				{
					qty = Integer.parseInt(qtyText);
					if (qty <= 0)
					{
						JOptionPane.showMessageDialog(this,
							"Ingredient row " + rowNum + ": Quantity must be a positive number.",
							"Validation Error", JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				catch (NumberFormatException e)
				{
					JOptionPane.showMessageDialog(this,
						"Ingredient row " + rowNum + ": Invalid quantity \"" + qtyText + "\"",
						"Validation Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				recipe.addIngredient(ingId, qty);
			}

			if (recipe.getIngredients().isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Add at least one ingredient.",
					"Validation Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Check for duplicate name (skip if editing the same recipe)
			boolean isRename = editingRecipe != null && !editingRecipe.getName().equals(rName);
			boolean isNew = editingRecipe == null;
			if ((isNew || isRename) && recipeManager.getRecipe(rName) != null)
			{
				JOptionPane.showMessageDialog(this,
					"A recipe named \"" + rName + "\" already exists. Choose a different name.",
					"Duplicate Name", JOptionPane.WARNING_MESSAGE);
				return;
			}

			recipeManager.addRecipe(recipe);
			saveData();
			refreshCombinations();
		}
	}

	private void rebuildCombinationsUi()
	{
		combinationsPanel.removeAll();

		List<CombinationRecipe> recipes = recipeManager.getAllRecipes();
		log.info("rebuildCombinationsUi: {} recipes to display", recipes.size());

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

		JButton refreshBtn = new JButton("\u21BB");
		refreshBtn.setToolTipText("Refresh prices");
		refreshBtn.addActionListener(e -> refreshCombinations());
		buttons.add(refreshBtn);

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

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		int totalIngredientCost = 0;
		for (Map.Entry<Integer, Integer> entry : recipe.getIngredients().entrySet())
		{
			int ingId = entry.getKey();
			int qty = entry.getValue();
			String ingName = getCachedName(ingId);

			int unitPrice = getCachedBuyPrice(ingId);
			int lineCost = unitPrice > 0 ? unitPrice * qty : 0;
			totalIngredientCost += lineCost;

			addLine(body, GP_FORMAT.format(qty) + "x " + ingName, ColorScheme.LIGHT_GRAY_COLOR);
			addLine(body, "  @ " + formatGp(unitPrice) + " = " + formatGp(lineCost), ColorScheme.LIGHT_GRAY_COLOR);
		}

		addLine(body, "---", ColorScheme.MEDIUM_GRAY_COLOR);

		String resultName = getCachedName(recipe.getResultItemId());
		int resultPrice = getCachedSellPrice(recipe.getResultItemId());
		int tax = resultPrice > 0 ? profitCalculator.calculateTax(resultPrice) : 0;
		int profit = profitCalculator.calculateCombinationProfit(recipe, totalIngredientCost, resultPrice);

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
}
