package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingMacroMenu extends KeyBinding {
    public KeybindingMacroMenu() {
        super("key.cyvplugin.macromenu.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}