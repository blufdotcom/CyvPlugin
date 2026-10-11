package net.cyvplugin.practice;

import com.google.gson.Gson;
import net.cyvplugin.macro.MacroStore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public final class MacroSaver {
    private MacroSaver() {}

    public static int saveRecorded(String name) {
        ArrayList<ArrayList<String>> ticks = RecordListener.recordedTicks;
        if (ticks == null || ticks.isEmpty()) return -1;

        int firstActive = 0;
        for (int i = 0; i < ticks.size(); i++) {
            if (isActiveStart(ticks.get(i))) {
                firstActive = i;
                break;
            }
        }

        int lastActive = 0;
        for (int i = ticks.size() - 1; i >= 0; i--) {
            if (isActiveEnd(ticks.get(i))) {
                lastActive = i + 1;
                break;
            }
        }

        if (lastActive <= firstActive) return 0;

        ArrayList<ArrayList<String>> trimmed = new ArrayList<>(ticks.subList(firstActive, lastActive));

        String target = normalizeName(name);
        File dir = new File(MacroStore.MACRO_PATH);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(MacroStore.MACRO_PATH + target + ".json");
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(new Gson().toJson(trimmed));
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        RecordListener.stopRecording();
        return trimmed.size();
    }

    private static boolean isActiveStart(ArrayList<String> tick) {
        if (tick == null || tick.size() < 10) return false;
        boolean w = Boolean.parseBoolean(tick.get(0));
        boolean a = Boolean.parseBoolean(tick.get(1));
        boolean s = Boolean.parseBoolean(tick.get(2));
        boolean d = Boolean.parseBoolean(tick.get(3));
        boolean jump = Boolean.parseBoolean(tick.get(4));
        boolean sneak = Boolean.parseBoolean(tick.get(6));
        float yaw = Float.parseFloat(tick.get(8));
        float pitch = Float.parseFloat(tick.get(9));
        return w || a || s || d || jump || sneak || yaw != 0 || pitch != 0;
    }

    private static boolean isActiveEnd(ArrayList<String> tick) {
        if (tick == null || tick.size() < 10) return false;
        boolean w = Boolean.parseBoolean(tick.get(0));
        boolean a = Boolean.parseBoolean(tick.get(1));
        boolean s = Boolean.parseBoolean(tick.get(2));
        boolean d = Boolean.parseBoolean(tick.get(3));
        boolean jump = Boolean.parseBoolean(tick.get(4));
        boolean sneak = Boolean.parseBoolean(tick.get(6));
        return w || a || s || d || jump || sneak;
    }

    private static String normalizeName(String name) {
        if (name == null) return "macro";
        String trimmed = name.trim();
        return trimmed.isEmpty() ? "macro" : trimmed;
    }
}