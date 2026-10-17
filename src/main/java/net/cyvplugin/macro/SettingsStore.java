package net.cyvplugin.macro;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class SettingsStore {
    public static final String SETTINGS_PATH = "config/cyvplugin/settings.txt";

    private static Boolean macroHudVisible;

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

    private static void load() {
        macroHudVisible = true;
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
            Files.write(file.toPath(),
                    ("macroHudVisible=" + macroHudVisible + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
