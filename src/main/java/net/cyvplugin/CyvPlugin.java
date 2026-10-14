package net.cyvplugin;

import net.cyvplugin.bridge.CyvThemeBridge;
import net.cyvplugin.command.CommandCyvPlugin;
import net.cyvplugin.event.MacroGuiKeyHandler;
import net.cyvplugin.event.MacroInputPriorityHandler;
import net.cyvplugin.event.MacroKeybindHandler;
import net.cyvplugin.event.MacroMenuKeyHandler;
import net.cyvplugin.event.MpkGuiButtonHandler;
import net.cyvplugin.event.PracticeKeyHandler;
import net.cyvplugin.event.PracticeTimingHandler;
import net.cyvplugin.event.TeleportKeyHandler;
import net.cyvplugin.update.UpdateNotificationHandler;
import net.cyvplugin.update.Updater;
import net.cyvplugin.practice.PracticeHandler;
import net.cyvplugin.practice.RecordListener;
import net.cyvplugin.practice.S08Watcher;
import net.cyvplugin.teleport.CheckpointHandler;
import net.cyvplugin.teleport.TPHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;

@Mod(modid = CyvPlugin.MODID, version = CyvPlugin.VERSION, dependencies = "required-after:cyvforge")
public class CyvPlugin {
    public static final String MODID = "cyvplugin";
    public static final String VERSION = "0.0.7";

    @Mod.Instance(CyvPlugin.MODID)
    public static CyvPlugin instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent preEvent) {
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new UpdateNotificationHandler());
        MinecraftForge.EVENT_BUS.register(new MacroMenuKeyHandler());
        MinecraftForge.EVENT_BUS.register(new MacroKeybindHandler());
        MinecraftForge.EVENT_BUS.register(new MacroGuiKeyHandler());
        MinecraftForge.EVENT_BUS.register(new MpkGuiButtonHandler());
        MinecraftForge.EVENT_BUS.register(new MacroInputPriorityHandler());
        MinecraftForge.EVENT_BUS.register(new S08Watcher());
        MinecraftForge.EVENT_BUS.register(new PracticeHandler());
        MinecraftForge.EVENT_BUS.register(new RecordListener());
        MinecraftForge.EVENT_BUS.register(new PracticeKeyHandler());
        MinecraftForge.EVENT_BUS.register(new PracticeTimingHandler());
        MinecraftForge.EVENT_BUS.register(new TPHandler());
        MinecraftForge.EVENT_BUS.register(new CheckpointHandler());
        MinecraftForge.EVENT_BUS.register(new TeleportKeyHandler());
        MacroMenuKeyHandler.registerKeybinding();
        MacroGuiKeyHandler.registerKeybinding();
        PracticeKeyHandler.registerKeybinding();
        TeleportKeyHandler.registerKeybindings();
        ClientCommandHandler.instance.registerCommand(new CommandCyvPlugin());
        ClientCommandHandler.instance.registerCommand(new net.cyvplugin.command.TPCommand());
        ClientCommandHandler.instance.registerCommand(new net.cyvplugin.command.TPFCommand());
        Updater.start();
        LogManager.getLogger().info("CyvPlugin mod initialized!");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent postEvent) {
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
