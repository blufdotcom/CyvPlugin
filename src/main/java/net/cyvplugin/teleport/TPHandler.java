package net.cyvplugin.teleport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TPHandler {

    private static double targetX;
    private static double targetY;
    private static double targetZ;
    private static float targetYaw;
    private static float targetPitch;
    private static boolean applyRotation = false;
    private static boolean waiting = false;
    private static int waitTicks = 0;
    private static final int TIMEOUT_TICKS = 200;
    private static final double ARRIVAL_THRESHOLD = 0.01;

    public static void startTracking(double x, double y, double z, float yaw, float pitch, boolean hasRotation) {
        targetX = x;
        targetY = y;
        targetZ = z;
        targetYaw = yaw;
        targetPitch = pitch;
        applyRotation = hasRotation;
        waiting = true;
        waitTicks = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!waiting) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            waiting = false;
            return;
        }

        double dx = Math.abs(player.posX - targetX);
        double dy = Math.abs(player.posY - targetY);
        double dz = Math.abs(player.posZ - targetZ);

        if (dx <= ARRIVAL_THRESHOLD && dy <= ARRIVAL_THRESHOLD && dz <= ARRIVAL_THRESHOLD) {
            if (applyRotation) {
                player.rotationYaw = targetYaw;
                player.rotationPitch = targetPitch;
                player.prevRotationYaw = targetYaw;
                player.prevRotationPitch = targetPitch;
            }
            waiting = false;
            return;
        }

        waitTicks++;
        if (waitTicks >= TIMEOUT_TICKS) {
            waiting = false;
        }
    }
}