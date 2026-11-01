package net.cyvplugin.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class CustomSounds {
    public static final String[] BUNDLED = {"chess", "fart", "boom", "click", "meowrgh", "miau", "wii", "shoot", "quack", "piano"};

    private static boolean registered;
    private static String signature = "";
    private static CyvSoundPack pack;

    public CustomSounds() {}

    public static File folder() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDataDir == null) return null;
        return new File(mc.mcDataDir, "config/cyvplugin/sounds");
    }

    public static void openFolder() {
        File folder = folder();
        if (folder == null) return;
        if (!folder.isDirectory()) folder.mkdirs();
        try {
            Desktop.getDesktop().open(folder);
        } catch (Exception ignored) {
        }
    }

    public static List<String> customNames() {
        File folder = folder();
        if (folder == null) return Collections.emptyList();
        List<String> names = CyvSoundPack.listOggNames(folder);
        String current = names.toString();
        if (!current.equals(signature)) {
            signature = current;
            if (registered) applyPack();
        }
        return names;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ensureRegistered();
    }

    private static void ensureRegistered() {
        if (registered) return;
        registered = true;
        File folder = folder();
        if (folder == null) return;
        if (!folder.isDirectory()) {
            folder.mkdirs();
            seedBundled(folder);
        }
        pack = new CyvSoundPack(folder);
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(new IResourceManagerReloadListener() {
                    @Override
                    public void onResourceManagerReload(IResourceManager manager) {
                        applyTo(manager);
                    }
                });
    }

    private static void seedBundled(File folder) {
        for (String name : BUNDLED) {
            File target = new File(folder, name + ".ogg");
            if (target.isFile()) continue;
            InputStream in = CustomSounds.class.getResourceAsStream("/assets/cyvplugin/sounds/" + name + ".ogg");
            if (in == null) continue;
            try {
                Files.copy(in, target.toPath());
            } catch (Exception ignored) {
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void applyPack() {
        if (pack == null) return;
        Minecraft.getMinecraft().getSoundHandler()
                .onResourceManagerReload(Minecraft.getMinecraft().getResourceManager());
    }

    private static void applyTo(IResourceManager manager) {
        ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
        Minecraft.getMinecraft().getSoundHandler().onResourceManagerReload(manager);
    }
}
