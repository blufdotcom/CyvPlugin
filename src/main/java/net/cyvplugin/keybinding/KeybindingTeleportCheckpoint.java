package net.cyvplugin.keybinding;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class KeybindingTeleportCheckpoint extends KeyBinding {
    public KeybindingTeleportCheckpoint() {
        super("key.cyvplugin.tpcheckpoint.desc", Keyboard.KEY_NONE, "key.cyvplugin.category");
    }
}