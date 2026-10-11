package net.cyvplugin.event;

import net.cyvplugin.keybinding.KeybindingPractice;
import net.cyvplugin.practice.PracticeHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PracticeKeyHandler {
    public static final KeybindingPractice KEY = new KeybindingPractice();

    public static void registerKeybinding() {
        ClientRegistry.registerKeyBinding(KEY);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        if (KEY.isPressed()) {
            PracticeHandler.toggle();
        }
    }
}