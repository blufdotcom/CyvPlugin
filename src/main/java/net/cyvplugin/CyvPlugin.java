package net.cyvplugin;

import net.cyvplugin.command.CommandCyvPlugin;
import net.cyvplugin.event.MacroKeybindHandler;
import net.cyvplugin.event.MacroMenuKeyHandler;
import net.cyvplugin.update.UpdateNotificationHandler;
import net.cyvplugin.update.Updater;
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
    public static final String VERSION = "0.0.4";

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
        MacroMenuKeyHandler.registerKeybinding();
        ClientCommandHandler.instance.registerCommand(new CommandCyvPlugin());
        Updater.start();
        LogManager.getLogger().info("CyvPlugin mod initialized!");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent postEvent) {
    }

    public static void sendChatMessage(String text) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("\u00a7a<CyvPlugin> \u00a7f" + text));
    }
}
