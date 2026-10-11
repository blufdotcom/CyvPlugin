package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingTeleportThroughBlock extends KeyBinding {
    public KeybindingTeleportThroughBlock() {
        super("key.cyvplugin.teleportthroughblock.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}