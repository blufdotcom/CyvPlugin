package net.cyvplugin.macro;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FavoritesStore {
    public static final String FAVORITES_PATH = "config/cyvplugin/favorites.txt";

    private FavoritesStore() {}

    public static Set<String> load() {
        Set<String> favorites = new LinkedHashSet<String>();
        try {
            File file = new File(FAVORITES_PATH);
            if (!file.isFile()) return favorites;
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String name = line.trim();
                if (!name.isEmpty()) favorites.add(name);
            }
        } catch (Exception ignored) {
        }
        return favorites;
    }

    public static void save(Set<String> favorites) {
        try {
            File file = new File(FAVORITES_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            List<String> lines = new ArrayList<String>(favorites);
            Collections.sort(lines);
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}