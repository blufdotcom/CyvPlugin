package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingSaveCheckpoint extends KeyBinding {
    public KeybindingSaveCheckpoint() {
        super("key.cyvplugin.savecheckpoint.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}