# AGENTS.md

CyvPlugin is a Minecraft 1.8.9 Forge mod. Mod ID `cyvplugin`.

## Build

- Toolchain is **ancient and fragile**: ForgeGradle 2.1-SNAPSHOT + Gradle 3.0 wrapper, MC `1.8.9-11.15.1.2318-1.8.9`, mappings `stable_20`.
- **Must build with JDK 8.** Gradle 3.0 will not run under JDK 9+. `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64` is already exported in this environment, but the bare `java` on `PATH` is JDK 21 — always go through `./gradlew` (it honors `JAVA_HOME`), never plain `java`.
- The proven ship workflow is `./gradlew build --no-daemon` (the forking daemon is memory-hungry), producing `build/libs/cyvplugin-<version>.jar`. If resolving ForgeGradle hits a network timeout on `files.minecraftforge.net`, rerun with `--offline` (ForgeGradle 2.1-SNAPSHOT is already cached locally).
- `./gradlew runClient` opens a full client (not viable headless).
- `./gradlew writeSha256` regenerates `build/cyvplugin-<version>.jar.sha256` next to the built jar.

## Self-update (GitHub Releases) — CONTRACT, frozen

The mod self-updates from `https://api.github.com/repos/blufdotcom/CyvPlugin/releases/latest` (`Updater.GITHUB_REPO`). The client is the single shipped jar, self-replacing, so **never change** the repo path, the release convention, or `net.cyvplugin.update.*` protocol unless a breaking migration is coordinated for old clients.

To publish an update, do **all** of these:

1. Bump `VERSION` in `src/main/java/net/cyvplugin/CyvPlugin.java` only (see Versioning below).
2. Run `./gradlew build --no-daemon --offline && ./gradlew writeSha256`.
3. Create a GitHub release with **tag `v<VERSION>`** (must match the source constant, e.g. `v1.1.0`), **not** marked as pre-release, with exactly two assets:
   - `build/libs/cyvplugin-<VERSION>.jar`
   - `build/cyvplugin-<VERSION>.jar.sha256`
   - e.g. `gh release create v1.1.0 build/libs/cyvplugin-1.1.0.jar build/cyvplugin-1.1.0.jar.sha256 --repo blufdotcom/CyvPlugin --title "v1.1.0"`
4. Never publish pre-releases to this endpoint unless you intend old clients to ignore them (they are skipped).

Client behavior: every time the main menu is shown (each launch and each return from a world), the client fetches the release JSON (TLS 1.2, User-Agent `CyvPlugin-Updater`), skips pre-releases, compares tag vs `VERSION` numerically (`Version.isNewer`), downloads the `.jar` asset, verifies its SHA-256 against the `.jar.sha256` asset **before** touching the live file, then atomically overwrites the running jar in place (safe on Linux/Android). `UpdateNotificationHandler` draws the live state on the main menu (`checking for updates...`, `v<new> downloading update...`, `v<new> downloaded - restart the game to apply`, `up to date`, or `CyvPlugin: <error>`). A mismatch or network failure aborts silently and surfaces as the error line (no file is replaced). Once downloaded (`updateReady`), later menu visits skip re-checking. The updater locates the running jar via the class code source, then Forge's `Loader` mod-container source, then the classpath.

## CyvForge integration (runtime, reflection)

CyvPlugin is **linked to CyvForge** and plays the macros CyvForge records. CyvForge is a **hard dependency**: `@Mod(dependencies = "required-after:cyvforge")` makes FML abort startup with a log if it is missing, so there are no runtime "not installed" guards anywhere. The macros live in `config/cyvforge/macros/<name>.json` (relative to the game dir — CyvForge's own `ConfigLoader.PATH` convention), a JSON array of ticks `[w][a][s][d][jump][sprint][sneak][yaw][pitch]`.

- `net.cyvplugin.bridge.CyvForgeBridge` calls into CyvForge **via reflection** (no compile-time dependency, so the internals may change at runtime without recompiling): to run a named macro it resolves `net.cyvforge.config.CyvClientConfig.getString(String,String)`/`set(String,Object)` (sets `currentMacro`, runs, then restores the previous value) and `net.cyvforge.command.mpk.CommandMacro.runMacro(String[])`; `stopMacro`/`isMacroRunning` use the static `macroRunning` field. `CommandMacro.runMacro` ignores its args and always plays `currentMacro`, so the config swap is required. Those symbols keep their plain names in CyvForge's reobfuscated jar (only Minecraft classes get SRG names), so reflection by name is stable. If resolution fails it throws (the game aborts) rather than silently degrading.
- `/cyvplugin macro run <name> | stop | list` (`CommandCyvPlugin`, registered in `init()` via `ClientCommandHandler.instance`) drives those macros. `list` and tab-completion read the `config/cyvforge/macros/` folder through `net.cyvplugin.macro.MacroStore`; `run`/`stop` delegate to CyvForge so its own `MacroListener` playback applies.
- `net.cyvplugin.bridge.CyvThemeBridge` reflects CyvForge's `CyvForge.theme` (`ColorTheme` int fields) and `net.cyvforge.util.GuiUtils` (`drawRoundedRect`/`drawString`/`drawCenteredString`) so CyvPlugin GUIs render with the exact Cyv style. Symbols keep their plain names in the reobfuscated jar.
- `KeybindingMacroMenu` (`net.cyvplugin.event.MacroMenuKeyHandler`) is registered unbound (`Keyboard.KEY_NONE`) in `init()`, user-assignable under the `key.cyvplugin.category` controls category, and opens `net.cyvplugin.gui.GuiMacroList` when pressed in-game. The list has a sticky search box (filter + favorites first); each row has a heart toggle on the far left (before the name) persisted in `config/cyvplugin/favorites.txt` via `net.cyvplugin.macro.FavoritesStore`, plus two right-side buttons shown only on the hovered row: `Run` (executes the macro and closes the menu) and a keybind chip (click to capture a key, Esc cancels, right-click clears, the chip shows `Keyboard.getKeyName` or `+`). Per-macro keybinds persist in `config/cyvplugin/keybinds.txt` via `net.cyvplugin.macro.KeybindStore` (`name=keyCode`) and are polled in-game by `net.cyvplugin.event.MacroKeybindHandler` (edge-detected `Keyboard.isKeyDown`, only when no screen is open) to run the macro through the bridge. Lang keys live in `assets/minecraft/lang/en_US.lang` (same convention as CyvForge).

## Versioning

The mod version is **parsed from source**: `build.gradle` regexes `VERSION = "..."` out of `src/main/java/net/cyvplugin/CyvPlugin.java`, and that same constant feeds the `@Mod` annotation. To bump the version, edit that constant only — `mcmod.info` and the jar name follow from it.

## No tests, no CI, no linter

- This repo has **no tests, CI workflows, formatter, or lint config**. The only automated verification is compiling (`./gradlew` compile or `build`).

## Code conventions

- **Do not add comments to the code unless asked or absolutely necessary.** New code should be self-explanatory without them.
- Code targets the **1.8.9 / MCP `stable_20` names**, not modern Mojmap: fields like `mc.thePlayer`, `mc.theWorld`, `world.getBlockState(pos)`, `KeyBinding.setKeyBindState`, LWJGL `Keyboard`/`Display` APIs. Do not "modernize" these.
- Mod main class is `CyvPlugin.java`; feature modules should be registered in its `init()`.
