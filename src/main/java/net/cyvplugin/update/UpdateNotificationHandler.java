package net.cyvplugin.update;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class UpdateNotificationHandler {
    @SubscribeEvent
    public void onMainMenu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiMainMenu) {
            Updater.start();
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (UpdateState.updateReady && UpdateState.newVersion != null) {
            drawMessage(mc, "CyvPlugin v" + UpdateState.newVersion + " downloaded - restart the game to apply", 0x55FF55);
        } else if (UpdateState.STATUS_CHECKING.equals(UpdateState.status)) {
            drawMessage(mc, "CyvPlugin checking for updates...", 0xAAAAAA);
        } else if (UpdateState.STATUS_DOWNLOADING.equals(UpdateState.status) && UpdateState.newVersion != null) {
            drawMessage(mc, "CyvPlugin v" + UpdateState.newVersion + " downloading update...", 0xFFAA00);
        } else if (UpdateState.STATUS_ERROR.equals(UpdateState.status) && UpdateState.error != null) {
            drawMessage(mc, "CyvPlugin: " + UpdateState.error, 0xFF5555);
        } else if (UpdateState.STATUS_UP_TO_DATE.equals(UpdateState.status)) {
            drawMessage(mc, "CyvPlugin up to date", 0xAAAAAA);
        }
    }

    private void drawMessage(Minecraft mc, String message, int color) {
        int width = mc.fontRendererObj.getStringWidth(message);
        int x = (mc.currentScreen.width - width) / 2;
        int y = 6;
        Gui.drawRect(x - 4, y - 3, x + width + 4, y + 10, 0x90000000);
        mc.fontRendererObj.drawStringWithShadow(message, x, y, color);
    }
}