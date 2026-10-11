package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingTeleportToTopBlock extends KeyBinding {
    public KeybindingTeleportToTopBlock() {
        super("key.cyvplugin.teleporttotopblock.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}