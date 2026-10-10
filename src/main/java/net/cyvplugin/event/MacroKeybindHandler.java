package net.cyvplugin.event;

import net.cyvplugin.CyvPlugin;
import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.macro.KeybindStore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;

import java.util.Map;

public class MacroKeybindHandler {
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState() || Keyboard.isRepeatEvent()) return;
        int key = Keyboard.getEventKey();
        if (key <= 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.currentScreen != null) return;
        for (Map.Entry<String, Integer> entry : KeybindStore.all().entrySet()) {
            Integer bound = entry.getValue();
            if (bound == null || bound != key) continue;
            try {
                CyvForgeBridge.runMacro(entry.getKey());
            } catch (Exception e) {
                LogManager.getLogger().error("CyvPlugin: failed to run macro '" + entry.getKey() + "'", e);
                CyvPlugin.sendChatMessage("Could not run macro '" + entry.getKey() + "': " + e.getMessage());
            }
            break;
        }
    }
}
