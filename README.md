# CyvPlugin

Addon for [CyvForge](https://github.com/Morpheye/CyvForge) (Minecraft 1.8.9 Forge) that adds a practice mode and unlock macro tools from CyvForge.

## Features

- **Practice mode:** client-side ghost player with real physics; your real body stays frozen. Hotbar: redstone (record), feather (fly), command block (save macro).
- **Macros:** record movement and save it as a CyvForge macro (`config/cyvforge/macros/<name>.json`). Run, stop and list macros. GUI with search, favorites and **Mirror**.
- **Auto-update:** downloads the latest GitHub release, verifies its SHA-256 and swaps the jar in `mods/` (on Windows the old jar is removed automatically once the game closes).

## Commands

Aliases: `/cyvplugin`, `/cyvp`

| Command | Description |
|---|---|
| `/cyvp practice` | Toggle practice mode |
| `/cyvp record` | Start recording |
| `/cyvp record save` | Save to the current macro |
| `/cyvp macro run <name>` | Run a macro |
| `/cyvp macro list` | List macros |
| `/tp <x> <y> <z> [yaw] [pitch]` | Teleport |
| `/tpf <yaw> [pitch]` | Set facing |

## Keybinds

Category **CyvPlugin Keys**, unbound by default: Macro List, Macro GUI, Practice Mode, Save Checkpoint, Teleport to Checkpoint, Teleport Through Block, Teleport to Top of Block.

## Install

Download `CyvPlugin.jar` from [Releases](https://github.com/blufdotcom/CyvPlugin/releases) and put it in `mods/` next to CyvForge — every update is just the same file again, so the previous copy is replaced instead of left behind. If you installed an older `cyvplugin-<version>.jar`, delete that file once.

## Build

```bash
./gradlew setupDecompWorkspace
./gradlew build        # build/libs/cyvplugin-<version>.jar
./gradlew writeSha256  # .jar.sha256 required by the updater
```
