package net.cyvplugin.event;

import net.cyvplugin.macro.SettingsStore;
import net.cyvplugin.practice.PracticeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MovementNoteHandler {
    private static final float[] SCALE = {1.0f, 1.122462f, 1.259921f, 1.33484f, 1.498307f, 1.6817928f};
    private static final float VOLUME = 0.7f;
    private static final int JUMP_INDEX = 4;

    private KeyBinding[] bindings;
    private final boolean[] prev = new boolean[SCALE.length];
    private final ISound[] playing = new ISound[SCALE.length];

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        update();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        update();
    }

    private void update() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) {
            for (int i = 0; i < prev.length; i++) {
                prev[i] = false;
                stop(mc, i);
            }
            return;
        }
        if (bindings == null) {
            GameSettings settings = mc.gameSettings;
            bindings = new KeyBinding[]{
                    settings.keyBindForward, settings.keyBindLeft, settings.keyBindBack,
                    settings.keyBindRight, settings.keyBindJump, settings.keyBindSneak
            };
        }
        boolean active = SettingsStore.isNoteSoundActive() && mc.currentScreen == null;
        boolean onlyJump = SettingsStore.isNotesOnlyJump();
        for (int i = 0; i < bindings.length; i++) {
            KeyBinding binding = bindings[i];
            boolean down = binding != null && binding.isKeyDown();
            boolean jumpPass = !onlyJump || binding == mc.gameSettings.keyBindJump;
            if (down && !prev[i] && active && jumpPass && !(onlyJump && i == JUMP_INDEX)) {
                play(mc, i, SCALE[i]);
            } else if (!down && prev[i]) {
                stop(mc, i);
            }
            prev[i] = down;
        }
    }

    /**onlyJump plays on every real jump instead of the press edge, so a held space
     * re-articulates the note each time the player leaves the ground (the press edge
     * is skipped in update() to avoid a double sound on the takeoff tick).
     * The entity check runs first: LivingJumpEvent also fires on the integrated
     * server thread for other entities, and those must return before touching
     * any client state.*/
    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.entityLiving != PracticeHandler.getControlledPlayer()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (!SettingsStore.isNoteSoundActive() || !SettingsStore.isNotesOnlyJump()) return;
        play(mc, JUMP_INDEX, SCALE[JUMP_INDEX]);
    }

    private void play(Minecraft mc, int index, float pitch) {
        stop(mc, index);
        ISound sound = new NoteSound(
                new ResourceLocation("cyvplugin", SettingsStore.getNotesSound()), pitch);
        playing[index] = sound;
        mc.getSoundHandler().playSound(sound);
    }

    private void stop(Minecraft mc, int index) {
        ISound sound = playing[index];
        if (sound == null) return;
        playing[index] = null;
        mc.getSoundHandler().stopSound(sound);
    }

    private static final class NoteSound implements ISound {
        private final ResourceLocation location;
        private final float pitch;

        private NoteSound(ResourceLocation location, float pitch) {
            this.location = location;
            this.pitch = pitch;
        }

        @Override
        public ResourceLocation getSoundLocation() { return location; }

        @Override
        public boolean canRepeat() { return false; }

        @Override
        public int getRepeatDelay() { return 0; }

        @Override
        public float getVolume() { return VOLUME; }

        @Override
        public float getPitch() { return pitch; }

        @Override
        public float getXPosF() { return 0.0F; }

        @Override
        public float getYPosF() { return 0.0F; }

        @Override
        public float getZPosF() { return 0.0F; }

        @Override
        public ISound.AttenuationType getAttenuationType() { return ISound.AttenuationType.NONE; }
    }
}
