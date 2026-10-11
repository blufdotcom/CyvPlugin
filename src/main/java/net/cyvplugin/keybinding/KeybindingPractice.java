package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingPractice extends KeyBinding {
    public KeybindingPractice() {
        super("key.cyvplugin.practice.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}