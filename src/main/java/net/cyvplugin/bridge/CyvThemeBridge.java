package net.cyvplugin.bridge;

import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CyvThemeBridge {
    private static Field themeField;
    private static Field color1Field;
    private static Field color2Field;
    private static Method drawRoundedRect;
    private static Method drawCenteredString;
    private static Method drawString;
    private static Method chatFormatting;
    private static Method drawColor;
    private static boolean resolved = false;

    private CyvThemeBridge() {}

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> cyvForge = findClass("net.cyvforge.CyvForge");
            themeField = cyvForge.getField("theme");
            Class<?> colorHelper = findClass("net.cyvforge.config.CyvClientColorHelper");
            color1Field = colorHelper.getField("color1");
            color2Field = colorHelper.getField("color2");
            Class<?> guiUtils = findClass("net.cyvforge.util.GuiUtils");
            drawRoundedRect = guiUtils.getMethod("drawRoundedRect", int.class, int.class, int.class, int.class, float.class, int.class);
            drawCenteredString = guiUtils.getMethod("drawCenteredString", String.class, int.class, int.class);
            drawString = guiUtils.getMethod("drawString", String.class, int.class, int.class, int.class, boolean.class);
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge is required but its API could not be resolved", e);
        }
    }

    public static int theme(String fieldName) {
        resolve();
        try {
            Object theme = themeField.get(null);
            return theme.getClass().getField(fieldName).getInt(theme);
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge theme field unavailable: " + fieldName, e);
        }
    }

    public static String chatColor1() {
        resolve();
        try {
            Object color = color1Field.get(null);
            if (chatFormatting == null) {
                chatFormatting = color.getClass().getMethod("getChatFormatting");
            }
            return (String) chatFormatting.invoke(color);
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge color1 unavailable", e);
        }
    }

    public static int getDrawColor(String which) {
        resolve();
        Field field = "color1".equals(which) ? color1Field : color2Field;
        try {
            Object color = field.get(null);
            if (drawColor == null) {
                drawColor = color.getClass().getMethod("getDrawColor");
            }
            return ((Long) drawColor.invoke(color)).intValue();
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge draw color unavailable: " + which, e);
        }
    }

    public static int lighten(int color, float factor) {
        if (factor < 0f) factor = 0f;
        if (factor > 1f) factor = 1f;
        int a = (color >>> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * factor);
        int g = (int) (((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * factor);
        int b = (int) ((color & 0xFF) + (255 - (color & 0xFF)) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawRoundedRect(int x1, int y1, int x2, int y2, float radius, int color) {
        resolve();
        try {
            drawRoundedRect.invoke(null, x1, y1, x2, y2, radius, color);
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge drawRoundedRect failed", e);
        }
    }

    public static int drawCenteredString(String text, int x, int y) {
        resolve();
        try {
            return (Integer) drawCenteredString.invoke(null, text, x, y);
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge drawCenteredString failed", e);
        }
    }

    public static int drawString(String text, int x, int y, int color, boolean shadow) {
        resolve();
        try {
            return (Integer) drawString.invoke(null, text, x, y, color, shadow);
        } catch (Exception e) {
            throw new IllegalStateException("CyvForge drawString failed", e);
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
