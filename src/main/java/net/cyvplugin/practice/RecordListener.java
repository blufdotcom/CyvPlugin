package net.cyvplugin.practice;

import net.cyvplugin.CyvPlugin;
import net.cyvplugin.bridge.CyvForgeBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;

public class RecordListener {

    public static boolean recording = false;
    public static double startX, startY, startZ;
    public static ArrayList<ArrayList<String>> recordedTicks = new ArrayList<>();
    public static ArrayList<ArrayList<String>> previousTicks = new ArrayList<>();

    private static float lastYaw = 0;
    private static float lastPitch = 0;
    private static float lastAbsYaw = 0;
    private static float lastAbsPitch = 0;
    private static boolean started = false;
    private static boolean leftStart = false;
    private static boolean waitingForGround = false;
    private static double prevX = 0, prevY = 0, prevZ = 0;

    public static void startRecording(double x, double y, double z) {
        recording = true;
        startX = x;
        startY = y;
        startZ = z;
        recordedTicks.clear();
        previousTicks.clear();
        started = false;
        leftStart = false;
        waitingForGround = false;
        lastAbsYaw = 0;
        lastAbsPitch = 0;
        prevX = x;
        prevY = y;
        prevZ = z;
    }

    public static void stopRecording() {
        recording = false;
        started = false;
        leftStart = false;
        waitingForGround = false;
        lastAbsYaw = 0;
        lastAbsPitch = 0;
        recordedTicks.clear();
        previousTicks.clear();
    }

    @SubscribeEvent
    public void onTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!recording) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = PracticeHandler.getControlledPlayer();
        if (player == null || mc.theWorld == null || mc.isGamePaused()) return;

        if (player.capabilities.isFlying) {
            stopRecording();
            CyvPlugin.sendChatMessage("Recording cancelled.");
            return;
        }

        GameSettings options = mc.gameSettings;
        MovementInput input = PracticeHandler.getControlledMovementInput();

        double dx = player.posX - startX;
        double dy = player.posY - startY;
        double dz = player.posZ - startZ;
        boolean atStart = Math.abs(dx) < 0.0001 && Math.abs(dy) < 0.0001 && Math.abs(dz) < 0.0001;

        if (!atStart) leftStart = true;

        if (leftStart && atStart) {
            double movedThisTick = Math.sqrt(
                    (player.posX - prevX) * (player.posX - prevX) +
                    (player.posZ - prevZ) * (player.posZ - prevZ)
            );
            boolean wasTeleported = movedThisTick > 1.0
                    && Math.abs(player.motionX) < 0.01
                    && Math.abs(player.motionZ) < 0.01;

            if (wasTeleported) {
                if (recordedTicks.size() >= 5) {
                    previousTicks = new ArrayList<>(recordedTicks);
                }
                recordedTicks.clear();
                started = false;
                leftStart = false;
                waitingForGround = true;
            }
        }

        prevX = player.posX;
        prevY = player.posY;
        prevZ = player.posZ;

        if (waitingForGround) {
            if (player.onGround) waitingForGround = false;
            return;
        }

        boolean w     = input.moveForward > 0;
        boolean s     = input.moveForward < 0;
        boolean a     = input.moveStrafe > 0;
        boolean d     = input.moveStrafe < 0;
        boolean jump  = input.jump;
        boolean sneak = input.sneak;
        boolean sprint = options.keyBindSprint.isKeyDown();

        boolean hasKey = w || a || s || d || jump || sneak;
        boolean validStart = hasKey
                || player.rotationYaw != lastYaw
                || player.rotationPitch != lastPitch;

        if (!started) {
            lastYaw = player.rotationYaw;
            lastPitch = player.rotationPitch;
            if (validStart) {
                started = true;
                if (!hasKey) return;
            } else {
                return;
            }
        }

        int decimals;
        try {
            decimals = CyvForgeBridge.getConfigInt("recordingDecimals", 3);
        } catch (Exception e) {
            decimals = 3;
        }
        float factor = (float) Math.pow(10, decimals);

        float prevYaw = recordedTicks.isEmpty() ? lastYaw : lastAbsYaw;
        float prevPitch = recordedTicks.isEmpty() ? lastPitch : lastAbsPitch;

        float yawChange = Math.round((player.rotationYaw - prevYaw) * factor) / factor;
        float pitchChange = Math.round((player.rotationPitch - prevPitch) * factor) / factor;

        ArrayList<String> tick = new ArrayList<>();
        tick.add(w + "");
        tick.add(a + "");
        tick.add(s + "");
        tick.add(d + "");
        tick.add(jump + "");
        tick.add(sprint + "");
        tick.add(sneak + "");
        tick.add(options.keyBindUseItem.isKeyDown() + "");
        tick.add(yawChange + "");
        tick.add(pitchChange + "");

        lastAbsYaw = player.rotationYaw;
        lastAbsPitch = player.rotationPitch;

        recordedTicks.add(tick);
    }
}
