package net.cyvplugin.event;

import net.cyvplugin.keybinding.KeybindingGuiMacro;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;

public class MacroGuiKeyHandler {
    public static final KeybindingGuiMacro KEY = new KeybindingGuiMacro();

    private static final String GUI_MACRO = "net.cyvforge.gui.GuiMacro";

    public static void registerKeybinding() {
        ClientRegistry.registerKeyBinding(KEY);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        if (KEY.isPressed()) {
            try {
                mc.displayGuiScreen((GuiScreen) Class.forName(GUI_MACRO).getConstructor().newInstance());
            } catch (Exception e) {
                LogManager.getLogger().error("CyvPlugin: failed to open CyvForge GuiMacro", e);
            }
        }
    }
}