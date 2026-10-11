package net.cyvplugin.event;

import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.bridge.CyvThemeBridge;
import net.cyvplugin.practice.PracticeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class PracticeTimingHandler {

    private static final String LABEL = "labelLastTiming";

    private static boolean wasEnabled;
    private static boolean wasVisible;

    private static PosTick lastTick = new PosTick();
    private static PosTick secondLastTick = new PosTick();
    private static int airtime;
    private static int lastMoveTime = -1;
    private static int lastGroundMoveTime = -1;
    private static int lastJumpTime = -1;
    private static int lastSprintTime = -1;
    private static int lastSneakTime = -2;
    private static boolean locked;
    private static boolean hasActed;
    private static int sidestep;
    private static int sidestepTime = -1;
    private static double vy;
    private static String lastTiming = "";
    private static long jumpTimestamp;
    private static long moveTimestamp;
    private static long sprintTimestamp;
    private static long releaseTimestamp;

    public static void onPracticeEnabled() {
        try {
            wasEnabled = CyvForgeBridge.isHudLabelEnabled(LABEL);
            Object label = CyvForgeBridge.findHudLabel(LABEL);
            wasVisible = label != null && label.getClass().getField("isVisible").getBoolean(label);
        } catch (Exception ignored) {
            wasEnabled = false;
            wasVisible = false;
        }
        try {
            CyvForgeBridge.setHudLabelVisible(LABEL, false);
        } catch (Exception ignored) {
        }
        lastTick = new PosTick();
        secondLastTick = new PosTick();
        airtime = 0;
        lastMoveTime = -1;
        lastGroundMoveTime = -1;
        lastJumpTime = -1;
        lastSprintTime = -1;
        lastSneakTime = -2;
        locked = false;
        hasActed = false;
        sidestep = 0;
        sidestepTime = -1;
        vy = 0;
        lastTiming = "";
        jumpTimestamp = 0;
        moveTimestamp = 0;
        sprintTimestamp = 0;
        releaseTimestamp = 0;
    }

    public static void onPracticeDisabled() {
        try {
            CyvForgeBridge.setHudLabelVisible(LABEL, wasVisible);
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!PracticeHandler.isActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        int keyCode = Keyboard.getEventKey();
        long eventTime = Keyboard.getEventNanoseconds();

        if (Keyboard.getEventKeyState()) {
            if (keyCode == mc.gameSettings.keyBindForward.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindLeft.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindRight.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
                moveTimestamp = eventTime;
            }
            if (keyCode == mc.gameSettings.keyBindJump.getKeyCode()) {
                jumpTimestamp = eventTime;
            }
            if (keyCode == mc.gameSettings.keyBindSprint.getKeyCode()) {
                sprintTimestamp = eventTime;
            }
        } else {
            if (keyCode == mc.gameSettings.keyBindForward.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindLeft.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindRight.getKeyCode() ||
                    keyCode == mc.gameSettings.keyBindBack.getKeyCode()) {
                releaseTimestamp = eventTime;
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !PracticeHandler.isActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = PracticeHandler.getControlledPlayer();
        if (player == null || mc.theWorld == null || mc.isGamePaused()) return;

        calculateLastTiming(mc.gameSettings, player);

        boolean[] keys = new boolean[] {mc.gameSettings.keyBindForward.isKeyDown(),
                mc.gameSettings.keyBindLeft.isKeyDown(),
                mc.gameSettings.keyBindBack.isKeyDown(), mc.gameSettings.keyBindRight.isKeyDown(),
                mc.gameSettings.keyBindJump.isKeyDown(), mc.gameSettings.keyBindSprint.isKeyDown(),
                mc.gameSettings.keyBindSneak.isKeyDown()};

        if ((!lastTick.onGround || !player.onGround) && !player.capabilities.isFlying) airtime++;

        vy = player.posY - lastTick.y;

        secondLastTick = lastTick;
        lastTick = new PosTick(player, airtime, keys);

        if (lastTick.onGround) {
            airtime = 0;
        }
    }

    private static void calculateLastTiming(GameSettings gameSettings, EntityPlayer player) {
        boolean showMS;
        try {
            showMS = CyvForgeBridge.getConfigBoolean("showMilliseconds", true);
        } catch (Exception e) {
            showMS = true;
        }

        if (gameSettings.keyBindForward.isKeyDown() || gameSettings.keyBindBack.isKeyDown() ||
                gameSettings.keyBindLeft.isKeyDown() || gameSettings.keyBindRight.isKeyDown()) {
            lastMoveTime++;
            lastGroundMoveTime++;
            hasActed = true;

            if (lastJumpTime > -1 && lastMoveTime == 0 && airtime != 0 && !(vy == 0 && lastTick.onGround)
                    && (lastTiming.contains("Pessi") || !locked)) {
                if ((lastJumpTime + 1) == 1) lastTiming = "Max Pessi";
                else lastTiming = "Pessi " + (lastJumpTime + 1) + " ticks";
                long diff = Math.abs(moveTimestamp - jumpTimestamp) / 1000000;

                if (showMS && diff < 500) {
                    lastTiming += " (" + diff + "ms)";
                }
                locked = true;
            }

            if (lastTick.onGround && !secondLastTick.onGround) {
                lastGroundMoveTime = 0;
            }

        } else {
            lastMoveTime = -1;
            lastGroundMoveTime = -1;
        }

        if (gameSettings.keyBindJump.isKeyDown() && airtime == 0) {
            lastJumpTime = 0;
            hasActed = true;

            if ((lastGroundMoveTime == 0 || lastMoveTime == 0) && !locked) {
                lastTiming = "Jam";

                long diff = Math.abs(jumpTimestamp - moveTimestamp) / 1000000;

                if (showMS && diff < 1000) {
                    lastTiming += " (" + diff + "ms)";
                }

                if (gameSettings.keyBindSprint.isKeyDown() || !gameSettings.keyBindForward.isKeyDown()) {
                    locked = true;
                }
            } else if (lastGroundMoveTime > -1 && !locked && lastJumpTime == 0) {
                if (lastSneakTime == -1) lastTiming = "Burst " + (lastGroundMoveTime - 1) + " ticks";
                else if (lastSneakTime > -1) lastTiming = "Burstjam " + (lastGroundMoveTime) + " ticks";
                else lastTiming = "HH " + (lastGroundMoveTime) + " ticks";

                long diff = (jumpTimestamp - moveTimestamp) / 1000000;
                if (showMS && Math.abs(diff) < 1000) {
                    lastTiming += " (" + diff + "ms)";
                }
                locked = true;
            }

        } else if (!lastTick.onGround && lastJumpTime > -1) {
            lastJumpTime++;
        } else {
            lastJumpTime = -1;
        }

        if (gameSettings.keyBindSneak.isKeyDown()) {
            if (lastSneakTime == -2) lastSneakTime = 0;
            else lastSneakTime++;
        } else {
            if (lastSneakTime == -1 || lastSneakTime == -2) lastSneakTime = -2;
            else lastSneakTime = -1;
        }

        if ((gameSettings.keyBindSprint.isKeyDown() || lastSprintTime != -1) && !lastTick.onGround) {
            lastSprintTime++;
            if (lastTiming.startsWith("Jam") && lastSprintTime == 0 && !locked && lastTick.keys[0]) {
                if (lastJumpTime >= 1) {
                    if (lastJumpTime == 1) lastTiming = "Max FMM";
                    else lastTiming = "FMM " + (lastJumpTime) + " ticks";

                    long diff = (sprintTimestamp - jumpTimestamp) / 1000000;
                    if (showMS && diff >= 0 && diff < 1000) lastTiming += " (" + diff + "ms)";

                    locked = true;
                }
            }
        } else {
            lastSprintTime = -1;
        }

        if (airtime > 0) {
            if (gameSettings.keyBindForward.isKeyDown() && !lastTick.keys[0] && (lastTick.keys[1] || lastTick.keys[3])) {
                long diff = (moveTimestamp - jumpTimestamp) / 1000000;
                String msTxt = (showMS && diff >= 0 && diff < 1000) ? " (" + diff + "ms)" : "";

                String markDisplay = getConfigString("markDisplay", "Sidestep");
                if ("Sidestep".equals(markDisplay)) {
                    sidestep = 2;
                    sidestepTime = airtime;
                } else {
                    lastTiming = "Mark " + airtime + "t" + msTxt;
                }
            }
        }

        if (getConfigBoolean("detectStrafejam", false) && airtime > 0) {
            if (gameSettings.keyBindForward.isKeyDown() && !(gameSettings.keyBindLeft.isKeyDown() || gameSettings.keyBindRight.isKeyDown())
                    && lastTick.keys[0] && (lastTick.keys[1] || lastTick.keys[3])) {

                long diff = (releaseTimestamp - jumpTimestamp) / 1000000;
                String msTxt = (showMS && diff >= 0 && diff < 1000) ? " (" + diff + "ms)" : "";
                String sjSuffix = "Strafejam " + (airtime) + "t" + msTxt;

                if (!getConfigBoolean("strafejamJamOnly", true)) {
                    if (lastTiming.isEmpty() || lastTiming.equals("-")) {
                        lastTiming = sjSuffix;
                    } else if (!lastTiming.contains("Strafejam")) {
                        lastTiming = lastTiming + " | " + sjSuffix;
                        locked = true;
                    }
                } else if (lastTiming.startsWith("Jam") || lastTiming.startsWith("Mark")) {
                    lastTiming = sjSuffix;
                    locked = true;
                }
            }
        }

        if (getConfigBoolean("detectWobble", false)) {
            String wobbledKey = "";

            if (gameSettings.keyBindForward.isKeyDown() && !lastTick.keys[0] && secondLastTick.keys[0]) wobbledKey += "W";
            if (gameSettings.keyBindLeft.isKeyDown() && !lastTick.keys[1] && secondLastTick.keys[1]) wobbledKey += "A";
            if (gameSettings.keyBindBack.isKeyDown() && !lastTick.keys[2] && secondLastTick.keys[2]) wobbledKey += "S";
            if (gameSettings.keyBindRight.isKeyDown() && !lastTick.keys[3] && secondLastTick.keys[3]) wobbledKey += "D";

            if (!wobbledKey.isEmpty()) {
                long diff = (moveTimestamp - releaseTimestamp) / 1000000;
                String msTxt = (showMS && diff >= 0 && diff < 1000) ? " " + diff + "ms" : "";
                lastTiming = "Wobble (" + wobbledKey + msTxt + ")";
            }
        }

        if (!(gameSettings.keyBindForward.isKeyDown() ||
                gameSettings.keyBindBack.isKeyDown() ||
                gameSettings.keyBindLeft.isKeyDown() ||
                gameSettings.keyBindRight.isKeyDown()) &&
                player.onGround) {
            locked = false;
            hasActed = false;
        }

        if (gameSettings.keyBindJump.isKeyDown() && airtime == 0) {
            if ((lastTick.strafe() != 0) && PracticeHandler.getControlledMovementInput().moveStrafe == 0) {
                sidestepTime = 1;
                sidestep = 0;
            } else if (PracticeHandler.getControlledMovementInput().moveStrafe != 0) {
                sidestepTime = 1;
                sidestep = 1;
            } else {
                sidestep = -1;
                sidestepTime = 0;
            }

        } else if (airtime > 0) {
            if (sidestep == -1 && PracticeHandler.getControlledMovementInput().moveStrafe != 0) {
                sidestep = 0;
                sidestepTime = airtime;
            }

            if (sidestepTime == airtime && PracticeHandler.getControlledMovementInput().moveStrafe == 0.0f && sidestep == 0) {
                sidestepTime++;
            }
        }

        if (lastJumpTime > 999) lastJumpTime = 999;
        if (lastGroundMoveTime > 999) lastGroundMoveTime = 999;
        if (lastMoveTime > 999) lastMoveTime = 999;
        if (lastSprintTime > 999) lastSprintTime = 999;
    }

    private static boolean getConfigBoolean(String key, boolean defaultValue) {
        try {
            return CyvForgeBridge.getConfigBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String getConfigString(String key, String defaultValue) {
        try {
            return CyvForgeBridge.getConfigString(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SubscribeEvent
    public void onRenderChat(RenderGameOverlayEvent.Pre.Chat event) {
        if (!PracticeHandler.isActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (mc.gameSettings.hideGUI) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiContainer)
                && !(mc.currentScreen instanceof GuiChat)) return;
        if (!wasEnabled || !wasVisible) return;

        int[] pos;
        try {
            pos = CyvForgeBridge.getHudLabelPosition(LABEL);
        } catch (Exception e) {
            return;
        }
        if (pos == null) return;

        int color1 = CyvThemeBridge.getDrawColor("color1");
        int color2 = CyvThemeBridge.getDrawColor("color2");
        FontRenderer font = mc.fontRendererObj;
        int x = pos[0] + 1;
        int y = pos[1] + 1;
        CyvThemeBridge.drawString("Last Timing: ", x, y, color1, true);
        CyvThemeBridge.drawString(lastTiming, x + font.getStringWidth("Last Timing: "), y, color2, true);
    }

    private static class PosTick {
        public boolean[] keys = new boolean[] {false, false, false, false, false, false, false};
        public double x;
        public double y;
        public double z;
        public boolean onGround = true;
        public int airtime;

        public PosTick() {
        }

        public PosTick(EntityPlayer player, int airtime, boolean[] keys) {
            this.x = player.posX;
            this.y = player.posY;
            this.z = player.posZ;
            this.onGround = player.onGround;
            this.airtime = airtime;
            this.keys = keys;
        }

        int strafe() {
            int i = 0;
            if (keys[1]) i--;
            if (keys[3]) i++;
            return i;
        }
    }
}