package net.cyvplugin.event;

import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.macro.KeybindStore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Map;

public class MacroKeybindHandler {
    private final boolean[] down = new boolean[256];

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            clear();
            return;
        }
        for (Map.Entry<String, Integer> entry : KeybindStore.all().entrySet()) {
            int key = entry.getValue();
            if (key <= 0 || key >= down.length) continue;
            boolean isDown = Keyboard.isKeyDown(key);
            if (isDown && !down[key] && mc.currentScreen == null) {
                try {
                    CyvForgeBridge.runMacro(entry.getKey());
                } catch (Exception ignored) {
                }
            }
            down[key] = isDown;
        }
    }

    private void clear() {
        for (int i = 0; i < down.length; i++) down[i] = false;
    }
}
