# CyvPlugin

Addon for [CyvForge](https://github.com/Morpheye/CyvForge) (Minecraft 1.8.9 Forge) that adds a practice mode and **unlocks** macro tools from CyvForge. Requires CyvForge.

## Features

- **Practice mode:** client-side ghost player; your real body stays frozen. Hotbar: record, fly, save macro.
- **Macros:** record movement and save it as a CyvForge macro (`config/cyvforge/macros/`). Run, stop and list macros. GUI with search, favorites, Mirror, and a key binding per macro.
- **Checkpoints and teleports:** save/teleport to a checkpoint, teleport through a block, teleport to the top of a block (all via keybinds).
- **Camera lean:** optional roll when strafing, adjustable angle.
- **Macro HUD:** optional on-screen display.
- **Jump note sounds:** plays a sound on movement keys or only on jumps. 10 sounds included; add your own `.ogg` files in `config/cyvplugin/sounds`.
- **Auto-update:** downloads the latest GitHub release, verifies its SHA-256 and applies it on restart.

## Commands

Aliases: `/cyvplugin`, `/cyvp`

| Command | Description |
|---|---|
| `/cyvp practice` | Toggle practice mode |
| `/cyvp record` | Start recording |
| `/cyvp record save` | Save to the current macro |
| `/cyvp macro run <name>` |
| `/cyvp macro stop` |
| `/cyvp macro list` |
| `/tp <x> <y> <z> [yaw] [pitch]` |
| `/tpf <yaw> [pitch]` | Set facing

## Keybinds

Under **Controls > CyvPlugin Keys**: 
- Open Macro List
- Open Macro GUI
- Toggle Practice Mode
- Save Checkpoint
- Teleport to Checkpoint
- Teleport Through Block
- Teleport to Top of Block.
_all unbound by defaul_

## Install

Download `CyvPlugin.jar` from [Releases](https://github.com/blufdotcom/CyvPlugin/releases) and put it in `mods/` next to CyvForge.

## Build

```bash
./gradlew setupDecompWorkspace
./gradlew build
./gradlew writeSha256
