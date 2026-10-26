package net.cyvplugin.bridge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CyvForgeBridge {
    private static final String COMMAND_MACRO = "net.cyvforge.command.mpk.CommandMacro";
    private static final String CLIENT_CONFIG = "net.cyvforge.config.CyvClientConfig";
    private static final String KEY_INPUT_HANDLER = "net.cyvforge.event.events.KeyInputHandler";
    private static final String RUN_MACRO_KEYBINDING = "net.cyvforge.keybinding.KeybindingRunMacro";
    private static final String HUD_MANAGER = "net.cyvforge.hud.HUDManager";
    private static final String TOGGLE_SPRINT = "net.cyvforge.keybinding.KeybindingTogglesprint";

    private CyvForgeBridge() {}

    private static Method findCall(Object target, String name, Class<?>... paramTypes) throws Exception {
        Class<?> owner = target instanceof Class ? (Class<?>) target : target.getClass();
        try {
            Method method = owner.getDeclaredMethod(name, paramTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException inherited) {
            Method method = owner.getMethod(name, paramTypes);
            method.setAccessible(true);
            return method;
        }
    }

    public static void runMacro(String name) throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        String previous = (String) findCall(config, "getString", String.class, String.class).invoke(config, "currentMacro", "");
        findCall(config, "set", String.class, Object.class).invoke(config, "currentMacro", name);
        Minecraft mc = Minecraft.getMinecraft();
        Field integratedServerRunning = mc.getClass().getDeclaredField("field_71455_al");
        Field theIntegratedServer = mc.getClass().getDeclaredField("field_71437_Z");
        integratedServerRunning.setAccessible(true);
        theIntegratedServer.setAccessible(true);
        boolean runningBackup = integratedServerRunning.getBoolean(mc);
        Object serverBackup = theIntegratedServer.get(mc);
        try {
            integratedServerRunning.setBoolean(mc, true);
            theIntegratedServer.set(mc, new IntegratedServer(mc, "CyvPlugin", "CyvPlugin",
                    new WorldSettings(0L, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT)));
            findCall(findClass(COMMAND_MACRO), "runMacro", String[].class).invoke(null, (Object) new String[0]);
        } finally {
            theIntegratedServer.set(mc, serverBackup);
            integratedServerRunning.setBoolean(mc, runningBackup);
            findCall(config, "set", String.class, Object.class).invoke(config, "currentMacro", previous);
        }
    }

    public static void runCurrentMacro() throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        String name = (String) findCall(config, "getString", String.class, String.class).invoke(config, "currentMacro", "macro");
        runMacro(name);
    }

    public static String getCurrentMacro() throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        return (String) findCall(config, "getString", String.class, String.class).invoke(config, "currentMacro", "macro");
    }

    public static int getConfigInt(String key, int defaultValue) throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        return (Integer) findCall(config, "getInt", String.class, int.class).invoke(config, key, defaultValue);
    }

    public static void setConfig(String key, String value) throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        findCall(config, "set", String.class, Object.class).invoke(config, key, value);
    }

    public static boolean getConfigBoolean(String key, boolean defaultValue) throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        return (Boolean) findCall(config, "getBoolean", String.class, boolean.class).invoke(config, key, defaultValue);
    }

    public static String getConfigString(String key, String defaultValue) throws Exception {
        Object config = findClass(CLIENT_CONFIG);
        return (String) findCall(config, "getString", String.class, String.class).invoke(config, key, defaultValue);
    }

    public static Object findHudLabel(String labelName) throws Exception {
        java.util.List<?> renderers = (java.util.List<?>) findClass(HUD_MANAGER).getField("registeredRenderers").get(null);
        if (renderers == null) return null;
        for (Object renderer : renderers) {
            if (renderer == null) continue;
            String name = (String) findCall(renderer, "getName").invoke(renderer);
            if (labelName.equals(name)) return renderer;
        }
        return null;
    }

    public static void setHudLabelVisible(String labelName, boolean visible) throws Exception {
        Object label = findHudLabel(labelName);
        if (label != null) label.getClass().getField("isVisible").setBoolean(label, visible);
    }

    public static boolean isHudLabelEnabled(String labelName) throws Exception {
        Object label = findHudLabel(labelName);
        return label != null && label.getClass().getField("isEnabled").getBoolean(label);
    }

    public static int[] getHudLabelPosition(String labelName) throws Exception {
        Object label = findHudLabel(labelName);
        if (label == null) return null;
        Object position = label.getClass().getField("position").get(label);
        if (position == null) return null;
        return new int[]{(Integer) findCall(position, "getAbsoluteX").invoke(position),
                (Integer) findCall(position, "getAbsoluteY").invoke(position)};
    }

    public static int getRunMacroKeyCode() throws Exception {
        Object binding = findRunMacroBinding();
        if (binding == null) return -1;
        return (Integer) findCall(binding, "func_151463_i").invoke(binding);
    }

    public static void consumeRunMacroKey() throws Exception {
        Object binding = findRunMacroBinding();
        if (binding != null) findCall(binding, "func_151468_f").invoke(binding);
    }

    private static Object findRunMacroBinding() throws Exception {
        Class<?> handler = findClass(KEY_INPUT_HANDLER);
        java.util.List<?> bindings = (java.util.List<?>) handler.getField("cyvKeybindings").get(null);
        if (bindings == null) return null;
        for (Object binding : bindings) {
            if (binding != null && binding.getClass().getName().equals(RUN_MACRO_KEYBINDING)) {
                return binding;
            }
        }
        return null;
    }

    public static void stopMacro() throws Exception {
        Field field = findClass(COMMAND_MACRO).getField("macroRunning");
        field.setInt(null, 1);
    }

    public static boolean isMacroRunning() throws Exception {
        Field field = findClass(COMMAND_MACRO).getField("macroRunning");
        return field.getInt(null) != 0;
    }

    public static boolean isToggleSprintOn() throws Exception {
        Field field = findClass(TOGGLE_SPRINT).getField("sprintToggled");
        return field.getBoolean(null);
    }

    /**Sprint flag of the macro's last line: [w][a][s][d][space][sprint][sneak][rmb][yaw][pitch].*/
    public static boolean lastMacroLineSprint() throws Exception {
        Field field = findClass(COMMAND_MACRO).getField("macro");
        java.util.List<?> macro = (java.util.List<?>) field.get(null);
        if (macro == null || macro.isEmpty()) return false;
        java.util.List<?> line = (java.util.List<?>) macro.get(macro.size() - 1);
        if (line == null || line.size() <= 5) return false;
        return Boolean.parseBoolean(String.valueOf(line.get(5)));
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