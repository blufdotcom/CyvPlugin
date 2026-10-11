package net.cyvplugin.event;

import net.cyvplugin.gui.GuiMacroList;
import net.cyvplugin.keybinding.KeybindingMacroMenu;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MacroMenuKeyHandler {
    public static final KeybindingMacroMenu KEY = new KeybindingMacroMenu();

    public static void registerKeybinding() {
        ClientRegistry.registerKeyBinding(KEY);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        if (KEY.isPressed()) {
            mc.displayGuiScreen(new GuiMacroList());
        }
    }
}