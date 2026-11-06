package net.cyvplugin.macro;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**The plugin's persistent per-macro data (favorites + keybinds) in a single
 * config/cyvplugin/data.txt:
 *
 * [favorites]
 * someMacro
 * [keybinds]
 * someMacro=24
 *
 * Installs that still have the old favorites.txt + keybinds.txt pair are migrated on
 * first load: both are merged into data.txt and the legacy files are then deleted.*/
public final class DataStore {
    public static final String DATA_PATH = "config/cyvplugin/data.txt";

    private static final String LEGACY_FAVORITES_PATH = "config/cyvplugin/favorites.txt";
    private static final String LEGACY_KEYBINDS_PATH = "config/cyvplugin/keybinds.txt";

    private static final String FAVORITES_SECTION = "[favorites]";
    private static final String KEYBINDS_SECTION = "[keybinds]";

    private static final Set<String> favorites = new LinkedHashSet<String>();
    private static final Map<String, Integer> keybinds = new LinkedHashMap<String, Integer>();
    private static boolean loaded;

    private DataStore() {}

    public static synchronized Set<String> getFavorites() {
        ensureLoaded();
        return new LinkedHashSet<String>(favorites);
    }

    public static synchronized void setFavorites(Collection<String> names) {
        ensureLoaded();
        favorites.clear();
        if (names != null) {
            for (String name : names) {
                if (name == null) continue;
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) favorites.add(trimmed);
            }
        }
        save();
    }

    public static synchronized Map<String, Integer> getKeybinds() {
        ensureLoaded();
        return new LinkedHashMap<String, Integer>(keybinds);
    }

    public static synchronized int getKeybind(String name) {
        ensureLoaded();
        Integer code = keybinds.get(name);
        return code == null ? -1 : code;
    }

    public static synchronized void setKeybind(String name, int keyCode) {
        ensureLoaded();
        keybinds.put(name, keyCode);
        save();
    }

    public static synchronized void removeKeybind(String name) {
        ensureLoaded();
        keybinds.remove(name);
        save();
    }

    public static synchronized void reload() {
        favorites.clear();
        keybinds.clear();
        loaded = true;
        if (new File(DATA_PATH).isFile()) parse(readLines(new File(DATA_PATH)));
        else migrateLegacy();
    }

    private static void ensureLoaded() {
        if (!loaded) reload();
    }

    private static void migrateLegacy() {
        File legacyFavorites = new File(LEGACY_FAVORITES_PATH);
        File legacyKeybinds = new File(LEGACY_KEYBINDS_PATH);
        boolean migrated = false;
        if (legacyFavorites.isFile()) {
            for (String line : readLines(legacyFavorites)) {
                String name = line.trim();
                if (!name.isEmpty()) favorites.add(name);
            }
            migrated = true;
        }
        if (legacyKeybinds.isFile()) {
            for (String line : readLines(legacyKeybinds)) parseKeybind(line);
            migrated = true;
        }
        if (!migrated) return;
        save();
        if (new File(DATA_PATH).isFile()) {
            legacyFavorites.delete();
            legacyKeybinds.delete();
        }
    }

    private static void parse(List<String> lines) {
        int section = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (FAVORITES_SECTION.equalsIgnoreCase(line)) {
                section = 1;
                continue;
            }
            if (KEYBINDS_SECTION.equalsIgnoreCase(line)) {
                section = 2;
                continue;
            }
            if (line.isEmpty()) continue;
            if (section == 1) favorites.add(line);
            else if (section == 2) parseKeybind(line);
        }
    }

    private static void parseKeybind(String line) {
        int eq = line.indexOf('=');
        if (eq <= 0) return;
        String name = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        if (name.isEmpty() || value.isEmpty()) return;
        try {
            keybinds.put(name, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
        }
    }

    private static List<String> readLines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return new ArrayList<String>();
        }
    }

    private static void save() {
        try {
            File file = new File(DATA_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            List<String> names = new ArrayList<String>(favorites);
            Collections.sort(names);
            List<String> lines = new ArrayList<String>();
            lines.add(FAVORITES_SECTION);
            lines.addAll(names);
            lines.add(KEYBINDS_SECTION);
            for (Map.Entry<String, Integer> entry : keybinds.entrySet()) {
                lines.add(entry.getKey() + "=" + entry.getValue());
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
