package net.cyvplugin.macro;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KeybindStore {
    public static final String KEYBINDS_PATH = "config/cyvplugin/keybinds.txt";

    private static final Map<String, Integer> binds = new LinkedHashMap<String, Integer>();
    private static boolean loaded = false;

    private KeybindStore() {}

    public static synchronized Map<String, Integer> all() {
        if (!loaded) reload();
        return binds;
    }

    public static synchronized int get(String name) {
        if (!loaded) reload();
        Integer code = binds.get(name);
        return code == null ? -1 : code;
    }

    public static synchronized void set(String name, int keyCode) {
        if (!loaded) reload();
        binds.put(name, keyCode);
        save();
    }

    public static synchronized void remove(String name) {
        if (!loaded) reload();
        binds.remove(name);
        save();
    }

    public static synchronized void reload() {
        binds.clear();
        loaded = true;
        try {
            File file = new File(KEYBINDS_PATH);
            if (!file.isFile()) return;
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String name = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (name.isEmpty() || value.isEmpty()) continue;
                try {
                    binds.put(name, Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try {
            File file = new File(KEYBINDS_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            List<String> lines = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : binds.entrySet()) {
                lines.add(entry.getKey() + "=" + entry.getValue());
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
