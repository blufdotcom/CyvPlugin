package net.cyvplugin.macro;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MacroStore {
    public static final String MACRO_PATH = "config/cyvforge/macros/";

    private MacroStore() {}

    public static List<String> listNames() {
        List<String> names = new ArrayList<String>();
        File dir = new File(MACRO_PATH);
        if (!dir.isDirectory()) return names;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return names;
        for (File file : files) {
            names.add(file.getName().substring(0, file.getName().length() - ".json".length()));
        }
        Collections.sort(names);
        return names;
    }
}