# OSRS Flip Plugin: Deployment & Testing Guide

This guide walks you through deploying the **OSRS Flip Plugin** into the RuneLite client for local testing. Since this plugin is not yet published to the official Plugin Hub, we will use RuneLite's **Local Plugin Development** mode.

---

## 📋 Prerequisites

Before starting, ensure you have the following:

1.  **Java Development Kit (JDK) 11 or higher**
    *   RuneLite requires JDK 11+.
    *   Verify installation: `java -version`
2.  **Git** (for cloning/managing the repo)
3.  **An Old School RuneScape Account**
    *   Can be a main account or a "ironman" test account.
    *   **Note:** You do **not** need a membership to test the UI, but you need an account to log in.
4.  **The Built JAR File**
    *   Ensure you have run `./gradlew build` successfully.
    *   Location: `osrs-flip-plugin/build/libs/osrs-flip-plugin.jar`

---

## 🚀 Step 1: Prepare the Plugin Artifact

Ensure your latest code changes are compiled into the JAR file.

```bash
# Navigate to project root
cd /path/to/osrs-flip-plugin

# Clean and build to ensure fresh artifacts
./gradlew clean build
```

**Verification:**
Check that the file exists:
```bash
ls -lh build/libs/osrs-flip-plugin.jar
```
*Expected Output:* A file roughly 10-20KB in size.

---

## 🔧 Step 2: Configure RuneLite for Local Plugins

RuneLite hides local plugins by default. We need to enable the developer mode.

### Option A: Command Line Launch (Recommended for Testing)
Launch RuneLite with the `-developer-mode` flag.

**Windows:**
1.  Find your `RuneLite.exe` or `RuneLite.jar`.
2.  If using the jar: `java -jar RuneLite.jar --developer-mode`
3.  If using the exe: Create a shortcut, right-click > Properties, and add ` --developer-mode` to the end of the "Target" field.

**macOS:**
1.  Open Terminal.
2.  Run: `/Applications/RuneLite.app/Contents/MacOS/RuneLite --developer-mode`
    *(Path may vary if installed elsewhere)*

**Linux:**
1.  Run: `./RuneLite.sh --developer-mode`

### Option B: Edit Settings File (Persistent)
1.  Locate your RuneLite settings folder:
    *   **Windows:** `%LOCALAPPDATA%\runelite\`
    *   **macOS:** `~/Library/Application Support/runelite/`
    *   **Linux:** `~/.runelite/`
2.  Open (or create) `settings.properties`.
3.  Add the line: `developerMode=true`
4.  Save and restart RuneLite normally.

---

## 📥 Step 3: Load the Plugin

Once RuneLite launches in **Developer Mode**:

1.  **Log in** to your OSRS account.
2.  Click the **Plugin Hub** icon (puzzle piece) on the side panel.
3.  Look for a new button at the top of the Plugin Hub panel: **"Develop Plugins"** (or a generic "Import" button depending on version).
    *   *Note: In newer versions, click the "wrench" icon or "Import Local Plugin" button.*
4.  Click **"Import Local Plugin"** (or similar).
5.  Navigate to your project folder and select:
    `osrs-flip-plugin/build/libs/osrs-flip-plugin.jar`
6.  RuneLite will verify the signature (skipped in dev mode) and load the plugin.
7.  You should see **"OSRS Flip Plugin"** appear in your list of installed plugins, likely under a "Local" or "Development" category.
8.  **Enable** the plugin by toggling the switch.

---

## 🧪 Step 4: Testing the Functionality

### 1. Verify Panel Appearance
*   Look at the side panel (usually on the right).
*   You should see a new icon (default RuneLite puzzle piece or custom if configured) labeled **"Flip Tracker"** or similar.
*   Click it to open the **OSRS Flip Panel**.

### 2. Test Item Search
*   In the panel, type "Burning claw" or "Feather".
*   Verify the autocomplete drops down and selects valid items.
*   **Action:** Add an item to your **Watchlist**.

### 3. Test Price Data
*   With an item on the watchlist, check the displayed prices.
*   **Buy Price:** Should match the current GE buy limit/price.
*   **Sell Price:** Should match the current GE sell price.
*   **Profit:** Should show `(Sell * 0.98) - Buy`.
*   *Note: If you are not logged into a world with GE access, it may use cached data.*

### 4. Test Manual Overrides
*   Click the "Edit" or "Override" button next to an item's price.
*   Enter a custom buy price (e.g., 100 gp).
*   Verify the profit calculation updates immediately using your manual price instead of the live GE price.

### 5. Test Combination Recipes
*   Navigate to the "Combinations" tab in the panel.
*   **Add Recipe:**
    *   Name: `Test Combo`
    *   Input 1: `Iron Ore` (Qty: 2)
    *   Input 2: `Coal` (Qty: 1)
    *   Output: `Steel Bar` (Qty: 1)
*   Save the recipe.
*   Check the profit calculation for this combination. It should factor in the cost of 2 Iron + 1 Coal vs the sell price of 1 Steel Bar (minus 2% tax).

### 6. Test Import/Export
*   Click "Export Watchlist" in the settings/menu of the panel.
*   Save the JSON file.
*   Clear your watchlist.
*   Click "Import Watchlist" and select the file.
*   Verify items reappear.

---

## ⚠️ Jagex Compliance & Safety Checklist

To ensure your account remains safe while testing:

| Feature | Status | Notes |
| :--- | :--- | :--- |
| **Input Automation** | ✅ **NONE** | The plugin does NOT click buttons or type for you. |
| **Overlay Highlights** | ✅ **NONE** | No items are highlighted in the 3D world or inventory. |
| **Live Polling** | ✅ **DISABLED** | Prices only update on GE open or manual refresh. |
| **Data Source** | ✅ **OFFICIAL** | Uses RuneLite's official `ItemManager` cache. |
| **UI Location** | ✅ **PANEL ONLY** | All info is contained in the side panel. |

**Rule of Thumb:** If the plugin requires you to look at a separate window (the panel) and make your own decisions, it is generally compliant. If it plays the game for you, it is bannable.

---

## 🐛 Troubleshooting

### "Plugin failed to load" or "Invalid Signature"
*   **Cause:** RuneLite is not in Developer Mode.
*   **Fix:** Restart RuneLite with the `--developer-mode` flag.

### "No prices found" for items
*   **Cause:** The item name might be slightly different, or the GE cache hasn't loaded.
*   **Fix:** Try searching for a common item like "Coins" or "Feather". Ensure you are logged into a world where the Grand Exchange is accessible (even if you don't open it).

### Panel not appearing
*   **Cause:** The plugin is installed but not enabled, or the icon is hidden.
*   **Fix:** Go to the Plugin Hub, find "OSRS Flip Plugin", and ensure the toggle is ON. Then click the "Configure" (wrench) icon to ensure "Sidebar Icon" is checked.

### Build Errors (`gradle build` fails)
*   **Cause:** Java version mismatch.
*   **Fix:** Ensure `JAVA_HOME` points to JDK 11 or 17. Run `java -version` to confirm.

---

## 🔄 Updating the Plugin

When you make code changes:

1.  Stop RuneLite (or disable the plugin in the client).
2.  Run `./gradlew clean build` in your terminal.
3.  In RuneLite, go to the Plugin Hub.
4.  Find the local plugin entry.
5.  Click the **Refresh/Reload** icon next to it (or remove and re-import the JAR).
6.  Re-enable the plugin.

---

## 📬 Next Steps: Publishing to Plugin Hub

Once testing is complete and you are ready for public release:

1.  **Fork the Plugin Hub Repo:** Go to [github.com/runelite/plugin-hub](https://github.com/runelite/plugin-hub).
2.  **Create a PR:** Add your plugin details to the `plugin-list.json` and host your JAR on a public URL (GitHub Releases).
3.  **Review Process:** RuneLite staff will review your code for safety and compliance.
4.  **Approval:** Once merged, users can install your plugin directly from the in-game Plugin Hub without developer mode.

**Good luck with your flipping!**

---

## Support

If you find this plugin useful, consider supporting development:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/ace554)
