package net.cyvplugin.macro;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

public final class MacroMirror {
    private MacroMirror() {}

    public static int mirror(String name) {
        File file = new File(MacroStore.MACRO_PATH + name + ".json");
        if (!file.isFile()) return -1;

        List<List<String>> ticks;
        try (FileReader reader = new FileReader(file)) {
            ticks = new Gson().fromJson(reader, new TypeToken<List<List<String>>>() {}.getType());
        } catch (Exception e) {
            return -1;
        }
        if (ticks == null) return -1;

        for (List<String> tick : ticks) {
            if (tick == null || tick.size() < 4) continue;

            String a = tick.get(1);
            tick.set(1, tick.get(3));
            tick.set(3, a);

            int yawIndex = tick.size() - 2;
            try {
                double yaw = Double.parseDouble(tick.get(yawIndex));
                tick.set(yawIndex, yaw == 0 ? "0.0" : (-yaw) + "");
            } catch (NumberFormatException e) {
                tick.set(yawIndex, "0.0");
            }
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(new Gson().toJson(ticks));
        } catch (Exception e) {
            return -1;
        }
        return ticks.size();
    }
}
