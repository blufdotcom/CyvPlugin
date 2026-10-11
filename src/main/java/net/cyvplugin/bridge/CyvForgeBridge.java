package net.cyvplugin.bridge;

import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CyvForgeBridge {
    private static final String COMMAND_MACRO = "net.cyvforge.command.mpk.CommandMacro";
    private static final String CLIENT_CONFIG = "net.cyvforge.config.CyvClientConfig";

    private CyvForgeBridge() {}

    public static void runMacro(String name) throws Exception {
        Class<?> config = findClass(CLIENT_CONFIG);
        Method getConfig = config.getMethod("getString", String.class, String.class);
        Method setConfig = config.getMethod("set", String.class, Object.class);
        Method run = findClass(COMMAND_MACRO).getMethod("runMacro", String[].class);
        String previous = (String) getConfig.invoke(null, "currentMacro", "macro");
        setConfig.invoke(null, "currentMacro", name);
        try {
            run.invoke(null, (Object) new String[0]);
        } finally {
            setConfig.invoke(null, "currentMacro", previous);
        }
    }

    public static void stopMacro() throws Exception {
        Field field = findClass(COMMAND_MACRO).getField("macroRunning");
        field.setInt(null, 1);
    }

    public static boolean isMacroRunning() throws Exception {
        Field field = findClass(COMMAND_MACRO).getField("macroRunning");
        return field.getInt(null) != 0;
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