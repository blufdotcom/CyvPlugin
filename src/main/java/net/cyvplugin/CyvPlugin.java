package net.cyvplugin;

import net.cyvplugin.bridge.CyvThemeBridge;
import net.cyvplugin.command.CommandCyvPlugin;
import net.cyvplugin.command.TPCommand;
import net.cyvplugin.command.TPFCommand;
import net.cyvplugin.event.CameraLeanHandler;
import net.cyvplugin.event.MovementNoteHandler;
import net.cyvplugin.event.MacroGuiKeyHandler;
import net.cyvplugin.event.MacroHudHandler;
import net.cyvplugin.event.MacroInputPriorityHandler;
import net.cyvplugin.event.MacroKeybindHandler;
import net.cyvplugin.event.MacroMenuKeyHandler;
import net.cyvplugin.event.MpkGuiButtonHandler;
import net.cyvplugin.event.PracticeKeyHandler;
import net.cyvplugin.event.TeleportKeyHandler;
import net.cyvplugin.update.UpdateNotificationHandler;
import net.cyvplugin.update.Updater;
import net.cyvplugin.practice.PracticeHandler;
import net.cyvplugin.practice.RecordListener;
import net.cyvplugin.practice.S08Watcher;
import net.cyvplugin.sound.CustomSounds;
import net.cyvplugin.teleport.CheckpointHandler;
import net.cyvplugin.teleport.TPHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = CyvPlugin.MODID, version = CyvPlugin.VERSION, dependencies = "required-after:cyvforge")
public class CyvPlugin {
    public static final String MODID = "cyvplugin";
    public static final String VERSION = "0.0.13";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(CyvPlugin.MODID)
    public static CyvPlugin instance;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        registerEventHandlers();
        registerKeybindings();
        registerCommands();
        Updater.start();
        LOGGER.info("CyvPlugin mod initialized!");
    }

    private void registerEventHandlers() {
        Object[] handlersBeforeHud = {
                new UpdateNotificationHandler(),
                new MacroMenuKeyHandler(),
                new MacroKeybindHandler(),
                new MacroGuiKeyHandler(),
                new MpkGuiButtonHandler(),
        };
        Object[] handlersAfterHud = {
                new MacroInputPriorityHandler(),
                new CameraLeanHandler(),
                new MovementNoteHandler(),
                new CustomSounds(),
                new S08Watcher(),
                new PracticeHandler(),
                new RecordListener(),
                new PracticeKeyHandler(),
                new TPHandler(),
                new CheckpointHandler(),
                new TeleportKeyHandler(),
        };
        for (Object handler : handlersBeforeHud) {
            MinecraftForge.EVENT_BUS.register(handler);
        }
        if (MacroHudHandler.install()) {
            MinecraftForge.EVENT_BUS.register(new MacroHudHandler());
        } else {
            LOGGER.warn("CyvPlugin: could not hook HUDManager, MACRO HUD toggle disabled");
        }
        for (Object handler : handlersAfterHud) {
            MinecraftForge.EVENT_BUS.register(handler);
        }
    }

    private void registerKeybindings() {
        MacroMenuKeyHandler.registerKeybinding();
        MacroGuiKeyHandler.registerKeybinding();
        PracticeKeyHandler.registerKeybinding();
        TeleportKeyHandler.registerKeybindings();
    }

    private void registerCommands() {
        ClientCommandHandler.instance.registerCommand(new CommandCyvPlugin());
        ClientCommandHandler.instance.registerCommand(new TPCommand());
        ClientCommandHandler.instance.registerCommand(new TPFCommand());
    }

    public static void sendChatMessage(String text) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(
                CyvThemeBridge.chatColor1() + "<CyvPlugin> \u00a7f" + text));
    }

    public static void sendChatMessageOnce(Object text, int id) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(
                CyvThemeBridge.chatColor1() + "<CyvPlugin> \u00a7f" + text), id);
    }
}
