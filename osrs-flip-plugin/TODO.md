# OSRS Flip Plugin — Future Work

## High Priority

### Recipe Manual Price Overrides
Manual price overrides only work for watchlist items. Recipe ingredients pull from the GE/wiki cache with no way to override. Need to add price override fields to the recipe edit dialog or allow per-ingredient price overrides inline.

### Distinct "No Data" vs "Loss" Display
When prices are unavailable (returning -1), the profit shows red — same as an actual loss. Add a distinct "N/A" or gray indicator for missing data vs red for confirmed negative profit.

### Item Search Optimization
Name search iterates ~30,000 items on the client thread with 300ms debounce. Consider caching item names on first search, or using the wiki API for name lookups, to reduce client thread load.

## Medium Priority

### Import/Export
Export watchlist + recipes to a JSON file via user dialog. Import from file. Referenced in the deployment guide but not implemented.

### Auto-Refresh on GE Open
Refresh prices when the Grand Exchange interface is opened in-game. Could use RuneLite's widget events to detect GE open.

### Config Group Migration
Config group is `"osrsflip"`. If ever renamed to something more specific, a migration must be provided per AGENTS.md rules to avoid resetting user settings.

## Low Priority

### Historical Price Tracking
Could show price trend sparklines or averages over time. Would require storing periodic price snapshots in the data file.

### Profit Notifications
Alert the user when a watched item crosses a profit threshold.

### Configurable Max Search Results
Currently hardcoded to 10 results. Could be a config option.

## Architecture Notes
- All item data fetching must happen on the RuneLite `ClientThread` (via `clientThread.invokeLater()`), then UI updates on the Swing EDT (via `SwingUtilities.invokeLater()`)
- HTTP requests must use OkHttp's `enqueue()` — never block the client thread
- `itemManager.getItemPrice()` must also be called on the client thread
- Data persists to `.runelite/osrs-flip-plugin/data.json` via `DataManager.java`
- GE tax: 2% floored, capped at 5M, 0 below 50gp — implemented in `ProfitCalculator.calculateTax()`
- Wiki prices fetched from `https://prices.runescape.wiki/api/v1/osrs/` (latest + 1h endpoints)
