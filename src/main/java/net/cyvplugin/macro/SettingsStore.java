package net.cyvplugin.macro;

import net.cyvplugin.sound.CustomSounds;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettingsStore {
    public static final String SETTINGS_PATH = "config/cyvplugin/settings.txt";
    public static final float LEAN_MIN = 0.1f;
    public static final float LEAN_MAX = 10.0f;
    public static final String NOTE_SOUND_DEFAULT = "none";

    private static Boolean macroHudVisible;
    private static Boolean leanEnabled;
    private static Float leanAngle;
    private static String notesSound;
    private static Boolean notesOnlyJump;

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

    public static synchronized String getNotesSound() {
        if (notesSound == null) load();
        return notesSound;
    }

    public static synchronized void setNotesSound(String sound) {
        if (notesSound == null) load();
        notesSound = isValidNoteSound(sound) ? sound : NOTE_SOUND_DEFAULT;
        save();
    }

    public static List<String> noteSounds() {
        List<String> sounds = new ArrayList<String>(CustomSounds.BUNDLED.length + 5);
        sounds.add(NOTE_SOUND_DEFAULT);
        Collections.addAll(sounds, CustomSounds.BUNDLED);
        try {
            for (String name : CustomSounds.customNames()) {
                if (!sounds.contains(name)) sounds.add(name);
            }
        } catch (Throwable ignored) {
        }
        return sounds;
    }

    public static boolean isValidNoteSound(String sound) {
        return noteSounds().contains(sound);
    }

    public static boolean isNoteSoundActive() {
        String sound = getNotesSound();
        return !"none".equals(sound) && isValidNoteSound(sound);
    }

    public static synchronized boolean isNotesOnlyJump() {
        if (notesOnlyJump == null) load();
        return notesOnlyJump;
    }

    public static synchronized void setNotesOnlyJump(boolean onlyJump) {
        if (notesOnlyJump == null) load();
        notesOnlyJump = onlyJump;
        save();
    }

    private static void load() {
        macroHudVisible = true;
        leanEnabled = false;
        leanAngle = 5.0f;
        notesSound = NOTE_SOUND_DEFAULT;
        notesOnlyJump = false;
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
                } else if ("notesSound".equals(key)) {
                    notesSound = isValidNoteSound(value) ? value : NOTE_SOUND_DEFAULT;
                } else if ("notesOnlyJump".equals(key)) {
                    notesOnlyJump = "true".equalsIgnoreCase(value);
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
                            + "notesSound=" + notesSound + "\n"
                            + "notesOnlyJump=" + notesOnlyJump + "\n"
            ).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
