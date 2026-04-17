package me.thukunua.thukuna.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class VideoAudioPlayer {

    private static final Identifier AUDIO_ID =
            Identifier.of("thukuna", "video_audio");

    private static boolean playing = false;

    public static void start() {
        if (playing) return;

        SoundEvent event = SoundEvent.of(AUDIO_ID);

        AbstractSoundInstance instance = new AbstractSoundInstance(
                event,
                SoundCategory.MASTER,
                net.minecraft.util.math.random.Random.create()
        ) {
            @Override
            public float getVolume() {
                return 1.0f;
            }

            @Override
            public float getPitch() {
                return 1.0f;
            }

            @Override
            public boolean isRepeatable() {
                return false;
            }
        };

        MinecraftClient.getInstance()
                .getSoundManager()
                .play(instance);

        playing = true;
    }

    public static void stop() {
        if (!playing) return;

        MinecraftClient.getInstance()
                .getSoundManager()
                .stopSounds(AUDIO_ID, null);

        playing = false;
    }
}