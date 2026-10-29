package net.cyvplugin.macro;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class SettingsStore {
    public static final String SETTINGS_PATH = "config/cyvplugin/settings.txt";
    public static final float LEAN_MIN = 0.1f;
    public static final float LEAN_MAX = 10.0f;

    private static Boolean macroHudVisible;
    private static Boolean leanEnabled;
    private static Float leanAngle;

    private SettingsStore() {}

    public static synchronized boolean isMacroHudVisible() {
        if (macroHudVisible == null) load();
        return macroHudVisible;
    }

    public static synchronized void setMacroHudVisible(boolean visible) {
        if (macroHudVisible == null) load();
        macroHudVisible = visible;
        save();
    }

    public static synchronized boolean isLeanEnabled() {
        if (leanEnabled == null) load();
        return leanEnabled;
    }

    public static synchronized void setLeanEnabled(boolean enabled) {
        if (leanEnabled == null) load();
        leanEnabled = enabled;
        save();
    }

    public static synchronized float getLeanAngle() {
        if (leanAngle == null) load();
        return leanAngle;
    }

    public static synchronized void setLeanAngle(float angle) {
        if (leanAngle == null) load();
        leanAngle = clampLean(angle);
        save();
    }

    public static float clampLean(float angle) {
        if (angle < LEAN_MIN) return LEAN_MIN;
        if (angle > LEAN_MAX) return LEAN_MAX;
        return angle;
    }

    private static void load() {
        macroHudVisible = true;
        leanEnabled = false;
        leanAngle = 5.0f;
        try {
            File file = new File(SETTINGS_PATH);
            if (!file.isFile()) return;
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if ("macroHudVisible".equals(key)) {
                    macroHudVisible = !"false".equalsIgnoreCase(value);
                } else if ("leanEnabled".equals(key)) {
                    leanEnabled = !"false".equalsIgnoreCase(value);
                } else if ("leanAngle".equals(key)) {
                    try {
                        leanAngle = clampLean(Float.parseFloat(value));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try {
            File file = new File(SETTINGS_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            Files.write(file.toPath(), (
                    "macroHudVisible=" + macroHudVisible + "\n"
                            + "leanEnabled=" + leanEnabled + "\n"
                            + "leanAngle=" + leanAngle + "\n"
            ).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
