package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingGuiMacro extends KeyBinding {
    public KeybindingGuiMacro() {
        super("key.cyvplugin.guimacro.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}