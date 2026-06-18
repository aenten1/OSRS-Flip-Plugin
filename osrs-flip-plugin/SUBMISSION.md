# Plugin Hub Submission Checklist

Pre-submission audit of this plugin against the [RuneLite Plugin Hub](https://github.com/runelite/plugin-hub)
requirements and the project's `AGENTS.md` rules. Check items off before opening the plugin-hub PR.

## 🔴 Blockers — build / CI will fail

- [~] **Plugin must be at the repository root.** Plugin Hub clones the repo at the manifest
      `commit=` and runs `./gradlew` at the **root**. The plugin is self-contained in
      `osrs-flip-plugin/` (no nested `.git`, no external deps). A standalone branch `plugin-only`
      has been prepared via `git subtree split` (full history preserved, plugin at root).
      **Remaining (yours):** create a public GitHub repo and push that branch as `main`:
      ```
      git push https://github.com/aenten1/<new-repo>.git plugin-only:main
      ```
      The unrelated root items (`07flip.tar.gz` 9-byte stub, empty `OpenSpec/` & `superpowers/`,
      root `README.md`/`DEPLOYMENT_GUIDE.md`) are excluded automatically by the split.
- [x] **`LICENSE` file (BSD 2-Clause)** added at the plugin root.
- [x] **Full BSD-2 header on every source file** (was: 7 files + the test had an
      "All rights reserved" stub instead of the permissive header).

## 🟠 Manual-review blockers

- [x] **Third-party-server toggle is opt-in + warned.** `useWikiPrices` now defaults to `false`
      and carries the required `warning`: *"This feature submits your IP address to a 3rd-party
      server not controlled or verified by RuneLite developers."*

## 🟡 Polish — likely reviewer comments

- [x] **Renamed to "Flip It"** (matches the in-app panel title). Updated `@PluginDescriptor`
      `name`, `runelite-plugin.properties` `displayName`, and the docs. Package, directory,
      `@ConfigGroup("osrsflip")`, and all keyNames were left intact (renaming them would reset
      users' settings).
- [x] **Author set** to `aenten1` in `runelite-plugin.properties` (was `Flip Developer`).
- [x] **Icon upgraded to 48×72** (gold-coin stack, transparent background, ~2.8 KB PNG).
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
