# OSRS Flip Plugin — Future Work

## High Priority

### Wiki Real-Time Price API
Pull prices from `https://prices.runescape.wiki/osrs/item/{itemId}` instead of RuneLite's cached `ItemManager.getItemPrice()`. The cached prices can be significantly stale for newer/volatile items (e.g. Oathplate Chest showed 290M vs actual 98M).

**Implementation notes:**
- Use `@Inject OkHttpClient` for HTTP requests (per AGENTS.md — never use HttpURLConnection)
- Use `enqueue()` to avoid blocking the client thread
- Add a `WikiPriceManager` or similar class
- The wiki API returns `high`, `highTime`, `low`, `lowTime` fields

**Config settings to add:**
- Moving average quantity (how many data points to average)
- Price mode: buy low, buy high, sell low, sell high
- Price source toggle: Wiki API vs ItemManager cache
- Refresh interval

### Recipe Manual Price Overrides
Currently, manual price overrides only work for watchlist items. Recipe ingredients pull from the GE cache with no way to override. Need to add price override fields to the recipe edit dialog or allow per-ingredient price overrides inline.

## Medium Priority

### Item Search by Name
Currently requires knowing item IDs. Add name-based search using `ItemManager` or the wiki API. Autocomplete dropdown would be ideal.

### Import/Export
Export watchlist + recipes to a JSON file. Import from file. UI buttons already referenced in the deployment guide but not implemented.

### Auto-Refresh on GE Open
Refresh prices when the Grand Exchange interface is opened in-game. Config flag exists in `config.schema.json` but not implemented.

## Low Priority

### Historical Price Tracking
`HistoricalPriceTracker.java` exists but is not wired into the UI. Could show price trend sparklines or avg over time.

### Profit Notifications
Alert the user when a watched item crosses a profit threshold.

## Architecture Notes
- All item data fetching must happen on the RuneLite `ClientThread` (via `clientThread.invokeLater()`), then UI updates on the Swing EDT (via `SwingUtilities.invokeLater()`)
- HTTP requests must use OkHttp's `enqueue()` — never block the client thread
- Data persists to `.runelite/osrs-flip-plugin/data.json` via `DataManager.java`
- GE tax: 2% floored, capped at 5M, 0 below 50gp — implemented in `ProfitCalculator.calculateTax()`
