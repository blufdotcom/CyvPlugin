package net.cyvplugin.event;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Field;

public class MpkGuiButtonHandler {
    private static final String GUI_MPK = "net.cyvforge.gui.GuiMPK";

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.gui;
        if (!gui.getClass().getName().equals(GUI_MPK)) return;
        try {
            Field field = gui.getClass().getDeclaredField("macroButton");
            field.setAccessible(true);
            Object button = field.get(gui);
            if (button == null) return;
            button.getClass().getMethod("setEnabled", boolean.class).invoke(button, true);
        } catch (Exception e) {
            LogManager.getLogger().error("CyvPlugin: failed to enable GuiMPK macro button", e);
        }
    }
}