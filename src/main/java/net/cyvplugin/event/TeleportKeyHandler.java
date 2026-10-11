package net.cyvplugin.event;

import net.cyvplugin.CyvPlugin;
import net.cyvplugin.keybinding.KeybindingSaveCheckpoint;
import net.cyvplugin.keybinding.KeybindingTeleportCheckpoint;
import net.cyvplugin.keybinding.KeybindingTeleportThroughBlock;
import net.cyvplugin.keybinding.KeybindingTeleportToTopBlock;
import net.cyvplugin.practice.PracticeHandler;
import net.cyvplugin.teleport.BlockTeleportHandler;
import net.cyvplugin.teleport.CheckpointHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TeleportKeyHandler {
    public static final KeybindingSaveCheckpoint SAVE = new KeybindingSaveCheckpoint();
    public static final KeybindingTeleportCheckpoint TP = new KeybindingTeleportCheckpoint();
    public static final KeybindingTeleportThroughBlock THROUGH = new KeybindingTeleportThroughBlock();
    public static final KeybindingTeleportToTopBlock TOP = new KeybindingTeleportToTopBlock();

    public static void registerKeybindings() {
        ClientRegistry.registerKeyBinding(SAVE);
        ClientRegistry.registerKeyBinding(TP);
        ClientRegistry.registerKeyBinding(THROUGH);
        ClientRegistry.registerKeyBinding(TOP);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        if (SaveCheckpointPressed()) return;
        if (TeleportCheckpointPressed()) return;
        if (ThroughBlockPressed()) return;
        if (TopBlockPressed()) return;
    }

    private static boolean blockedByPractice() {
        if (PracticeHandler.isActive()) {
            CyvPlugin.sendChatMessage("TP disabled in practice mode.");
            return true;
        }
        return false;
    }

    private static boolean SaveCheckpointPressed() {
        if (!SAVE.isPressed()) return false;
        if (blockedByPractice()) return true;
        CheckpointHandler.saveCheckpoint();
        return false;
    }

    private static boolean TeleportCheckpointPressed() {
        if (!TP.isPressed()) return false;
        if (blockedByPractice()) return true;
        CheckpointHandler.teleportToCheckpoint();
        return false;
    }

    private static boolean ThroughBlockPressed() {
        if (!THROUGH.isPressed()) return false;
        if (blockedByPractice()) return true;
        BlockTeleportHandler.teleportThroughBlock();
        return false;
    }

    private static boolean TopBlockPressed() {
        if (!TOP.isPressed()) return false;
        if (blockedByPractice()) return true;
        BlockTeleportHandler.teleportToTopOfBlock();
        return false;
    }
}