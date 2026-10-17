package net.cyvplugin.event;

import net.cyvplugin.bridge.CyvForgeBridge;
import net.cyvplugin.macro.SettingsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces CyvForge's HUDManager event listener so the "MACRO" overlay text can be
 * hidden without touching chat or the rest of CyvForge's HUD.
 */
public class MacroHudHandler {

    private static final String HUD_MANAGER = "net.cyvforge.hud.HUDManager";
    private static final String DRAGGABLE = "net.cyvforge.hud.structure.DraggableHUDElement";

    private static final ConcurrentHashMap<String, Method> BOOL_METHODS = new ConcurrentHashMap<String, Method>();

    private static Object hudInstance;
    private static Method hudRender;
    private static Method callRenderer;
    private static Field registeredRenderers;
    private static Class<?> guiModConfigClass;
    private static Class<?> guiMpKClass;
    private static volatile boolean installed;

    public static boolean install() {
        try {
            Class<?> hudClass = findClass(HUD_MANAGER);
            Method render = hudClass.getMethod("render", float.class);
            Field renderers = hudClass.getField("registeredRenderers");
            Method call = hudClass.getDeclaredMethod("callRenderer", findClass(DRAGGABLE), float.class);
            call.setAccessible(true);
            Class<?> guiModConfig = tryClass("net.cyvforge.gui.GuiModConfig");
            Class<?> guiMpK = tryClass("net.cyvforge.gui.GuiMPK");
            Object instance = findHudManagerInstance();
            if (instance == null) return false;

            MinecraftForge.EVENT_BUS.unregister(instance);

            hudInstance = instance;
            hudRender = render;
            registeredRenderers = renderers;
            callRenderer = call;
            guiModConfigClass = guiModConfig;
            guiMpKClass = guiMpK;
            installed = true;
            return true;
        } catch (Exception e) {
            installed = false;
            return false;
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Pre.Chat event) {
        if (!installed) return;
        boolean hide = false;
        try {
            hide = !SettingsStore.isMacroHudVisible() && CyvForgeBridge.isMacroRunning();
        } catch (Exception ignored) {
        }
        if (!hide) {
            renderOriginal(event.partialTicks);
            return;
        }
        try {
            renderWithoutMacroText(event.partialTicks);
        } catch (Exception e) {
            renderOriginal(event.partialTicks);
        }
    }

    private static void renderOriginal(float partialTicks) {
        try {
            hudRender.invoke(hudInstance, partialTicks);
        } catch (Exception ignored) {
        }
    }

    private static void renderWithoutMacroText(float partialTicks) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.hideGUI) return;

        GuiScreen current = mc.currentScreen;
        boolean inScreen = current == null || current instanceof GuiContainer
                || current instanceof GuiChat
                || (guiModConfigClass != null && guiModConfigClass.isInstance(current))
                || (guiMpKClass != null && guiMpKClass.isInstance(current));
        if (!inScreen) return;

        Object renderers = registeredRenderers.get(null);
        if (!(renderers instanceof List)) return;
        for (Object renderer : (List<?>) renderers) {
            if (renderer == null) continue;
            if (current instanceof GuiContainer && !invokeBoolean(renderer, "renderInGui")) continue;
            if (current instanceof GuiChat && !invokeBoolean(renderer, "renderInChat")) continue;
            if (mc.gameSettings.showDebugInfo && !invokeBoolean(renderer, "renderInOverlay")) continue;
            callRenderer.invoke(null, renderer, partialTicks);
        }
    }

    private static boolean invokeBoolean(Object target, String name) throws Exception {
        String key = target.getClass().getName() + "." + name;
        Method method = BOOL_METHODS.get(key);
        if (method == null) {
            method = target.getClass().getMethod(name);
            BOOL_METHODS.put(key, method);
        }
        return (Boolean) method.invoke(target);
    }

    private static Object findHudManagerInstance() throws Exception {
        Field listenersField = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("listeners");
        listenersField.setAccessible(true);
        Object map = listenersField.get(MinecraftForge.EVENT_BUS);
        if (!(map instanceof ConcurrentHashMap)) return null;
        for (Object key : ((ConcurrentHashMap<?, ?>) map).keySet()) {
            if (key != null && key.getClass().getName().equals(HUD_MANAGER)) return key;
        }
        return null;
    }

    private static Class<?> tryClass(String name) {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException first) {
            try {
                return Class.forName(name, false, Loader.instance().getModClassLoader());
            } catch (ClassNotFoundException ignored) {
                throw first;
            }
        }
    }
}
