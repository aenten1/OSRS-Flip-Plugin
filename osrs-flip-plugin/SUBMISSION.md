# Plugin Hub Submission Checklist

Pre-submission audit of this plugin against the [RuneLite Plugin Hub](https://github.com/runelite/plugin-hub)
requirements and the project's `AGENTS.md` rules. Check items off before opening the plugin-hub PR.

## 🔴 Blockers — build / CI will fail

- [ ] **Plugin must be at the repository root.** Plugin Hub clones the repo at the manifest
      `commit=` and runs `./gradlew` at the **root**. Today the plugin lives in `osrs-flip-plugin/`
      with unrelated files at the git root (`07flip.tar.gz`, `superpowers`, `OpenSpec`,
      `DEPLOYMENT_GUIDE.md`). Publish a **dedicated public repo** whose root is this directory's
      contents (`build.gradle`, `settings.gradle`, `runelite-plugin.properties`, `src/`, `LICENSE`,
      `gradlew`, etc. at the top level).
- [x] **`LICENSE` file (BSD 2-Clause)** added at the plugin root.
- [x] **Full BSD-2 header on every source file** (was: 7 files + the test had an
      "All rights reserved" stub instead of the permissive header).

## 🟠 Manual-review blockers

- [x] **Third-party-server toggle is opt-in + warned.** `useWikiPrices` now defaults to `false`
      and carries the required `warning`: *"This feature submits your IP address to a 3rd-party
      server not controlled or verified by RuneLite developers."*

## 🟡 Polish — likely reviewer comments

- [ ] **Rename the plugin.** Hub style discourages "OSRS"/"RuneScape"/"Plugin" in display names.
      Pick a cleaner name (e.g. "Flip Tracker") and update **both** the `@PluginDescriptor` `name`
      in `OsrsFlipPlugin.java` and `displayName` in `runelite-plugin.properties` so they match.
- [ ] **Real identity.** Set `author=` in `runelite-plugin.properties` to your GitHub username
      (currently `Flip Developer`). Must match the copyright holder in the headers/LICENSE.
- [ ] **Higher-res icon.** `icon.png` is 16×16; the hub convention is **48×72**. Provide a crisper
      icon for the plugin browser listing.
- [ ] **(Optional) Disk I/O off the client thread.** `DataManager.save()` runs synchronously in
      `shutDown()` (client thread). It's a tiny JSON file and widely tolerated, but moving it off
      the client thread would fully satisfy the "no blocking disk I/O on the client thread" rule.

## ✅ Verified clean (no action needed)

- No reflection in the prohibited sense (`gson.reflect.TypeToken` / `java.lang.reflect.Type` are
  standard Gson generics).
- No `Process`/`ProcessBuilder`, JNI/JNA, `Unsafe`, dynamic classloading, or Java serialization.
- HTTP via OkHttp `enqueue()` with an `@Inject`ed `OkHttpClient`; responses closed; off the client thread.
- `@Inject Gson` (derived via `gson.newBuilder()`); no hand-rolled `new Gson()`.
- File I/O confined to `RuneLite.RUNELITE_DIR/osrs-flip-plugin/`.
- No `META-INF/services/net.runelite.client.plugins.Plugin` file.
- Java 11 target, UTF-8 encoding.
- Config group `"osrsflip"` is specific (not the `example` template default).
- `shutDown()` cancels the scheduled task, `shutdownNow()`s the executor, removes the nav button.
- No build artifacts committed (the tracked `gradle/wrapper/gradle-wrapper.jar` is expected).
- `log.debug` only — no per-event `log.info` spam.
- Feature (a GE price/profit side panel) falls in no forbidden category
  (boss/PvP/menu/input/privacy).

## Opening the plugin-hub PR

After the blockers above are cleared and the plugin builds at its repo root:

1. Fork `runelite/plugin-hub`, create a branch.
2. Add a file under `plugins/` (name = your plugin's id) containing:
   ```
   repository=https://github.com/<you>/<plugin-repo>
   commit=<40-char commit hash>
   ```
3. Push and open a PR describing the plugin.
4. If CI fails, fix in your plugin repo, update the `commit=` hash, and push to the same PR.
