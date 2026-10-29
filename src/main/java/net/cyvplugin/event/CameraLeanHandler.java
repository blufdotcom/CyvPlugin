package net.cyvplugin.event;

import net.cyvplugin.macro.SettingsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CameraLeanHandler {
    private static final float SMOOTH = 0.18f;

    private float smoothedRoll;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!SettingsStore.isLeanEnabled()) {
            smoothedRoll = 0f;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            smoothedRoll = 0f;
            return;
        }
        float angle = SettingsStore.getLeanAngle();
        float target = 0f;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) target -= angle;
        if (mc.gameSettings.keyBindRight.isKeyDown()) target += angle;
        smoothedRoll += (target - smoothedRoll) * SMOOTH;
        event.roll += smoothedRoll;
    }
}
