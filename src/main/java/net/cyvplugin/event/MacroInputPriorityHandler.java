package net.cyvplugin.event;

import net.cyvplugin.bridge.CyvForgeBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class MacroInputPriorityHandler {
    private KeyBinding[] bindings;
    private boolean wasRunning;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        boolean running = isRunning();
        if (running || wasRunning) {
            pressPhysical();
        }
        if (event.phase == TickEvent.Phase.END) {
            wasRunning = running;
        }
    }

    private void pressPhysical() {
        try {
            if (bindings == null) {
                GameSettings settings = Minecraft.getMinecraft().gameSettings;
                bindings = new KeyBinding[]{
                        settings.keyBindForward, settings.keyBindLeft, settings.keyBindBack,
                        settings.keyBindRight, settings.keyBindJump, settings.keyBindSprint,
                        settings.keyBindSneak, settings.keyBindUseItem
                };
            }
            for (KeyBinding binding : bindings) {
                if (binding == null) continue;
                int code = binding.getKeyCode();
                if (code == 0) continue;
                if (Keyboard.isKeyDown(code) && !binding.isKeyDown()) {
                    KeyBinding.setKeyBindState(code, true);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isRunning() {
        try {
            return CyvForgeBridge.isMacroRunning();
        } catch (Exception e) {
            return false;
        }
    }
}