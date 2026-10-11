package net.cyvplugin.practice;

import com.mojang.authlib.GameProfile;
import net.cyvplugin.CyvPlugin;
import net.cyvplugin.bridge.CyvForgeBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

public class PracticeHandler {

    private static final int PRACTICE_CHAT_ID = 0x5A5A;

    private static final int SLOT_RECORD = 0;
    private static final int SLOT_FLY = 4;
    private static final int SLOT_SAVE = 8;

    private static final String PRACTICE_MACRO_NAME = "practice";

    private static boolean active;
    private static PracticePlayer ghost;

    private static ItemStack recordBlock;
    private static ItemStack flyFeather;
    private static ItemStack saveBlock;
    private static int itemNameFlashTicks;
    private static int lastGhostSlot = -1;

    private static MovementInput savedInput;
    private static PlayerControllerMP savedController;
    private static float saveYaw, savePitch, savePrevYaw, savePrevPitch;
    private static int savedBodySlot;

    private static double repPosX, repPosY, repPosZ;
    private static int repTicks;

    public static void toggle() {
        if (active) disable();
        else enable();
    }

    public static void enable() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || active) return;
        if (ghost != null) disable();

        saveYaw = player.rotationYaw;
        savePitch = player.rotationPitch;
        savePrevYaw = player.prevRotationYaw;
        savePrevPitch = player.prevRotationPitch;
        savedInput = player.movementInput;

        ghost = new PracticePlayer(mc.theWorld, player.getGameProfile(), mc);
        ghost.setLocationAndAngles(player.posX, player.posY, player.posZ, saveYaw, savePitch);
        ghost.prevRotationYawHead = player.prevRotationYawHead;
        ghost.rotationYawHead = player.rotationYawHead;
        ghost.prevSwingProgress = 0.0F;
        ghost.swingProgress = 0.0F;
        ensureFreeSpawn(ghost);
        if (!mc.theWorld.spawnEntityInWorld(ghost)) {
            ghost = null;
            return;
        }
        active = true;

        savedBodySlot = player.inventory.currentItem;
        recordBlock = new ItemStack(Items.redstone);
        recordBlock.setStackDisplayName("\u00A7eRecord macro");
        flyFeather = new ItemStack(Items.feather);
        flyFeather.setStackDisplayName("\u00A7cFly off");
        saveBlock = new ItemStack(Item.getItemFromBlock(Blocks.command_block));
        saveBlock.setStackDisplayName("\u00A7aSave macro (" + PRACTICE_MACRO_NAME + ")");
        ghost.inventory.setInventorySlotContents(SLOT_RECORD, recordBlock);
        ghost.inventory.setInventorySlotContents(SLOT_FLY, flyFeather);
        ghost.inventory.setInventorySlotContents(SLOT_SAVE, saveBlock);
        ghost.inventory.currentItem = SLOT_RECORD;
        lastGhostSlot = SLOT_RECORD;
        ghost.capabilities.allowFlying = false;
        ghost.capabilities.isFlying = false;
        itemNameFlashTicks = 40;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        player.movementInput = new MovementInput();
        player.moveStrafing = 0.0F;
        player.moveForward = 0.0F;
        player.setJumping(false);

        repPosX = player.posX;
        repPosY = player.getEntityBoundingBox().minY;
        repPosZ = player.posZ;
        repTicks = 0;

        player.setInvisible(true);

        savedController = mc.playerController;
        mc.playerController = new PracticePlayerController(mc, mc.getNetHandler());

        mc.setRenderViewEntity(ghost);
        CyvPlugin.sendChatMessageOnce("\u00a7aPractice: ON", PRACTICE_CHAT_ID);
        net.cyvplugin.event.PracticeTimingHandler.onPracticeEnabled();
    }

    public static void disable() {
        Minecraft mc = Minecraft.getMinecraft();
        PracticePlayer g = ghost;
        ghost = null;
        boolean wasOn = active || g != null;
        try {
            if (g != null) {
                g.setDead();
                if (g.worldObj != null) g.worldObj.removeEntity(g);
                World w = mc.theWorld;
                if (w != null && w != g.worldObj) w.removeEntity(g);
                if (w != null) w.getChunkFromChunkCoords(g.chunkCoordX, g.chunkCoordZ).removeEntity(g);
            }
        } finally {
            if (wasOn) {
                EntityPlayerSP player = mc.thePlayer;
                if (player != null) {
                    mc.setRenderViewEntity(player);
                    player.setInvisible(false);
                    if (savedInput != null) player.movementInput = savedInput;
                    player.rotationYaw = saveYaw;
                    player.rotationPitch = savePitch;
                    player.prevRotationYaw = savePrevYaw;
                    player.prevRotationPitch = savePrevPitch;
                }
                if (savedController != null) {
                    mc.playerController = savedController;
                    savedController = null;
                }
                savedInput = null;
                CyvPlugin.sendChatMessageOnce("\u00a7cPractice: OFF", PRACTICE_CHAT_ID);
                net.cyvplugin.event.PracticeTimingHandler.onPracticeDisabled();
            }
            active = false;
            repTicks = 0;
            recordBlock = null;
            flyFeather = null;
            saveBlock = null;
            itemNameFlashTicks = 0;
            lastGhostSlot = -1;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static EntityPlayer getControlledPlayer() {
        Minecraft mc = Minecraft.getMinecraft();
        return active && ghost != null && !ghost.isDead ? ghost : mc.thePlayer;
    }

    public static MovementInput getControlledMovementInput() {
        Minecraft mc = Minecraft.getMinecraft();
        return active && ghost != null && !ghost.isDead ? ghost.movementInput : mc.thePlayer.movementInput;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || ghost == null || ghost.isDead) {
            disable();
            return;
        }
        if (itemNameFlashTicks > 0) --itemNameFlashTicks;
        if (ghost.inventory.getStackInSlot(SLOT_RECORD) != recordBlock) ghost.inventory.setInventorySlotContents(SLOT_RECORD, recordBlock);
        if (ghost.inventory.getStackInSlot(SLOT_FLY) != flyFeather) ghost.inventory.setInventorySlotContents(SLOT_FLY, flyFeather);
        if (ghost.inventory.getStackInSlot(SLOT_SAVE) != saveBlock) ghost.inventory.setInventorySlotContents(SLOT_SAVE, saveBlock);
        if (ghost.inventory.currentItem != lastGhostSlot) {
            lastGhostSlot = ghost.inventory.currentItem;
            flashSelectedItem();
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindDrop.getKeyCode(), false);
        player.moveStrafing = 0.0F;
        player.moveForward = 0.0F;
        player.setJumping(false);
        reportBodyToServer(player);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null || ghost == null || ghost.isDead) return;
        ghost.rotationYaw = player.rotationYaw;
        ghost.rotationPitch = player.rotationPitch;
        ghost.rotationYawHead = player.rotationYawHead;
        ghost.prevRotationYaw = ghost.rotationYaw;
        ghost.prevRotationPitch = ghost.rotationPitch;
        ghost.prevRotationYawHead = ghost.rotationYawHead;
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        if (!active) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;

        if (event.button == -1) {
            if (mc.thePlayer != null) mc.thePlayer.inventory.currentItem = savedBodySlot;
            if (ghost != null) ghost.inventory.changeCurrentItem(event.dwheel);
            event.setCanceled(true);
            return;
        }

        if (event.button != 1 || !event.buttonstate) return;
        if (ghost != null) {
            if (ghost.inventory.currentItem == SLOT_RECORD) startRecording();
            else if (ghost.inventory.currentItem == SLOT_FLY) toggleFlyPermission();
            else if (ghost.inventory.currentItem == SLOT_SAVE) saveMacro();
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!active) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || ghost == null) return;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindDrop.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindInventory.getKeyCode(), false);
        for (int l = 0; l < 9; l++) {
            KeyBinding kb = mc.gameSettings.keyBindsHotbar[l];
            if (kb.isPressed()) {
                ghost.inventory.currentItem = l;
                KeyBinding.setKeyBindState(kb.getKeyCode(), false);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderHandEvent(RenderHandEvent event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderGameOverlayText(RenderGameOverlayEvent.Text event) {
        if (!active || itemNameFlashTicks <= 0 || ghost == null) return;
        ItemStack selected = getSelectedItem();
        if (selected == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = event.resolution;
        String name = selected.getDisplayName();
        int x = (sr.getScaledWidth() - mc.fontRendererObj.getStringWidth(name)) / 2;
        int y = sr.getScaledHeight() - 59;
        int alpha = Math.min((int) ((float) itemNameFlashTicks * 256.0F / 10.0F), 255);
        if (alpha > 0) {
            mc.fontRendererObj.drawStringWithShadow(name, x, y, 0xFFFFFF | (alpha << 24));
        }
    }

    /**The ghost's own hotbar is never drawn (the visible one belongs to the real
     * body), so the selected practice item is announced by a fading name instead.*/
    private static ItemStack getSelectedItem() {
        if (ghost == null) return null;
        ItemStack stack = ghost.inventory.getStackInSlot(ghost.inventory.currentItem);
        if (stack == recordBlock || stack == flyFeather || stack == saveBlock) return stack;
        return null;
    }

    private static void flashSelectedItem() {
        if (getSelectedItem() != null) itemNameFlashTicks = 40;
    }

    private static void startRecording() {
        if (ghost == null) return;
        RecordListener.startRecording(ghost.posX, ghost.posY, ghost.posZ);
        CyvPlugin.sendChatMessage("Recording started.");
    }

    private static void saveMacro() {
        int ticks = MacroSaver.saveRecorded(PRACTICE_MACRO_NAME);
        if (ticks <= 0) {
            CyvPlugin.sendChatMessage("Nothing recorded to save.");
            return;
        }
        try {
            CyvForgeBridge.setConfig("currentMacro", PRACTICE_MACRO_NAME);
        } catch (Exception ignored) {
        }
        CyvPlugin.sendChatMessage("Recording saved to " + PRACTICE_MACRO_NAME + ".json (" + ticks + " ticks) and selected.");
    }

    private static void toggleFlyPermission() {
        if (ghost == null) return;
        ghost.capabilities.allowFlying = !ghost.capabilities.allowFlying;
        if (!ghost.capabilities.allowFlying) ghost.capabilities.isFlying = false;
        flyFeather.setStackDisplayName(ghost.capabilities.allowFlying ? "\u00A7aFly on" : "\u00A7cFly off");
        itemNameFlashTicks = 40;
    }

    private static void reportBodyToServer(EntityPlayerSP player) {
        if (player.isRiding()) {
            player.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(saveYaw, savePitch, player.onGround));
            player.sendQueue.addToSendQueue(new C0CPacketInput(player.moveStrafing, player.moveForward, player.movementInput.jump, player.movementInput.sneak));
            return;
        }
        double d0 = player.posX - repPosX;
        double d1 = player.getEntityBoundingBox().minY - repPosY;
        double d2 = player.posZ - repPosZ;
        boolean moved = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || repTicks >= 20;
        player.sendQueue.addToSendQueue(moved
                ? new C03PacketPlayer.C04PacketPlayerPosition(player.posX, player.getEntityBoundingBox().minY, player.posZ, player.onGround)
                : new C03PacketPlayer(player.onGround));
        ++repTicks;
        if (moved) {
            repPosX = player.posX;
            repPosY = player.getEntityBoundingBox().minY;
            repPosZ = player.posZ;
            repTicks = 0;
        }
    }

    private static void ensureFreeSpawn(PracticePlayer ghost) {
        World world = ghost.worldObj;
        AxisAlignedBB bb = ghost.getEntityBoundingBox();
        List<AxisAlignedBB> collisions = world.getCollidingBoundingBoxes(ghost, bb);
        if (collisions.isEmpty()) return;

        double eps = 1.0E-9D;
        double px = ghost.posX, py = ghost.posY, pz = ghost.posZ;
        double bestDist = Double.MAX_VALUE;
        double bestX = 0.0D, bestY = 0.0D, bestZ = 0.0D;

        double[][] dirs = {{1.0D, 0.0D, 0.0D}, {-1.0D, 0.0D, 0.0D},
                {0.0D, 1.0D, 0.0D}, {0.0D, -1.0D, 0.0D},
                {0.0D, 0.0D, 1.0D}, {0.0D, 0.0D, -1.0D}};
        double[] pushes = new double[6];

        for (int i = 0; i < 6; i++) {
            double need = 0.0D;
            for (AxisAlignedBB col : collisions) {
                double d;
                if (dirs[i][0] > 0.0D) d = col.minX - bb.maxX;
                else if (dirs[i][0] < 0.0D) d = bb.minX - col.maxX;
                else if (dirs[i][1] > 0.0D) d = col.minY - bb.maxY;
                else if (dirs[i][1] < 0.0D) d = bb.minY - col.maxY;
                else if (dirs[i][2] > 0.0D) d = col.minZ - bb.maxZ;
                else d = bb.minZ - col.maxZ;
                if (d > need) need = d;
            }
            pushes[i] = need + eps;
        }

        for (int mask = 1; mask < 64; mask++) {
            double dx = 0.0D, dy = 0.0D, dz = 0.0D;
            boolean bad = false;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) == 0) continue;
                if (i < 2 && (mask & (1 << (1 - i))) != 0) bad = true;
                if (i >= 2 && i < 4 && (mask & (1 << (5 - i))) != 0) bad = true;
                if (i >= 4 && (mask & (1 << (9 - i))) != 0) bad = true;
                dx += dirs[i][0] * pushes[i];
                dy += dirs[i][1] * pushes[i];
                dz += dirs[i][2] * pushes[i];
            }
            if (bad) continue;
            if (world.getCollidingBoundingBoxes(ghost, bb.offset(dx, dy, dz)).isEmpty()) {
                double dist = dx * dx + dy * dy + dz * dz;
                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = dx;
                    bestY = dy;
                    bestZ = dz;
                }
            }
        }

        if (bestDist < Double.MAX_VALUE) {
            ghost.setLocationAndAngles(px + bestX, py + bestY, pz + bestZ, ghost.rotationYaw, ghost.rotationPitch);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (active) disable();
    }
}