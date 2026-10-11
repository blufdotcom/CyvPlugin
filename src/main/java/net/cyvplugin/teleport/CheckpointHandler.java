package net.cyvplugin.teleport;

import net.cyvplugin.CyvPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Locale;

public class CheckpointHandler {

    private static boolean checkpointSet = false;
    private static double checkpointX = 0.0;
    private static double checkpointY = 0.0;
    private static double checkpointZ = 0.0;
    private static double checkpointYaw = 0.0;
    private static double checkpointPitch = 0.0;

    private static double targetX;
    private static double targetY;
    private static double targetZ;
    private static float targetYaw;
    private static float targetPitch;

    private static boolean waitingForTurn = false;
    private static int turnTicks = 0;
    private static final int TURN_TIMEOUT_TICKS = 200;
    private static final double ARRIVAL_THRESHOLD = 0.01;

    public static void saveCheckpoint() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        if (player == null || mc.theWorld == null) return;

        checkpointSet = true;
        checkpointX = player.posX;
        checkpointY = player.posY;
        checkpointZ = player.posZ;
        checkpointYaw = player.rotationYaw;
        checkpointPitch = player.rotationPitch;

        CyvPlugin.sendChatMessage("Checkpoint saved");
    }

    public static void teleportToCheckpoint() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        if (player == null || mc.theWorld == null) return;
        if (!checkpointSet) return;

        targetX = checkpointX;
        targetY = checkpointY;
        targetZ = checkpointZ;
        targetYaw = (float) checkpointYaw;
        targetPitch = (float) checkpointPitch;

        if (isExactlyAtDestination(player)) {
            applyViewRotation(player);
            return;
        }

        player.sendChatMessage("/tp " + String.format(Locale.US, "%.16f", targetX) + " " + String.format(Locale.US, "%.16f", targetY) + " " + String.format(Locale.US, "%.16f", targetZ));

        waitingForTurn = true;
        turnTicks = 0;
    }

    private static boolean isExactlyAtDestination(EntityPlayerSP player) {
        double dx = Math.abs(player.posX - targetX);
        double dy = Math.abs(player.posY - targetY);
        double dz = Math.abs(player.posZ - targetZ);
        return dx == 0.0 && dy == 0.0 && dz == 0.0;
    }

    private static boolean isAtDestination(EntityPlayerSP player) {
        double dx = Math.abs(player.posX - targetX);
        double dy = Math.abs(player.posY - targetY);
        double dz = Math.abs(player.posZ - targetZ);
        return dx <= ARRIVAL_THRESHOLD && dy <= ARRIVAL_THRESHOLD && dz <= ARRIVAL_THRESHOLD;
    }

    private static void applyViewRotation(EntityPlayerSP player) {
        if (player == null) return;
        player.rotationYaw = targetYaw;
        player.rotationPitch = targetPitch;
        player.prevRotationYaw = targetYaw;
        player.prevRotationPitch = targetPitch;
        waitingForTurn = false;
        turnTicks = 0;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!waitingForTurn) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) {
            waitingForTurn = false;
            turnTicks = 0;
            return;
        }

        if (isAtDestination(player)) {
            applyViewRotation(player);
            return;
        }

        turnTicks++;
        if (turnTicks >= TURN_TIMEOUT_TICKS) {
            waitingForTurn = false;
            turnTicks = 0;
        }
    }

    @SubscribeEvent
    public void onWorldChange(PlayerEvent.PlayerLoggedOutEvent event) {
        clearCheckpoint();
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft().theWorld == null) {
            clearCheckpoint();
        }
    }

    private static void clearCheckpoint() {
        checkpointSet = false;
        checkpointX = 0.0;
        checkpointY = 0.0;
        checkpointZ = 0.0;
        checkpointYaw = 0.0;
        checkpointPitch = 0.0;
        targetX = 0.0;
        targetY = 0.0;
        targetZ = 0.0;
        targetYaw = 0.0f;
        targetPitch = 0.0f;
        waitingForTurn = false;
        turnTicks = 0;
    }
}