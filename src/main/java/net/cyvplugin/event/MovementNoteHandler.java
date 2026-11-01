package net.cyvplugin.event;

import net.cyvplugin.macro.SettingsStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class MovementNoteHandler {
    private static final float[] SCALE = {1.0f, 1.122462f, 1.259921f, 1.33484f, 1.498307f, 1.6817928f};
    private static final float VOLUME = 0.7f;

    private KeyBinding[] bindings;
    private final boolean[] prev = new boolean[SCALE.length];

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
            for (int i = 0; i < prev.length; i++) prev[i] = false;
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
            if (down && !prev[i] && active && jumpPass) play(mc, SCALE[i]);
            prev[i] = down;
        }
    }

    private void play(Minecraft mc, float pitch) {
        mc.getSoundHandler().playSound(new NoteSound(
                new ResourceLocation("cyvplugin", SettingsStore.getNotesSound()), pitch));
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
