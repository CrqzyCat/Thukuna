package me.thukunua.thukuna.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class VideoAudioPlayer {

    private static final Identifier AUDIO_ID = Identifier.of("thukuna", "video_audio");
    private static AbstractSoundInstance currentInstance = null;

    public static void start() {
        stop(); // Sicherstellen dass kein alter Sound laeuft

        SoundEvent event = SoundEvent.of(AUDIO_ID);

        currentInstance = new AbstractSoundInstance(event, SoundCategory.MASTER, Random.create()) {
            @Override public float getVolume() { return 1.0f; }
            @Override public float getPitch()  { return 1.0f; }
            @Override public boolean isRepeatable() { return false; }
        };

        MinecraftClient.getInstance().getSoundManager().play(currentInstance);
    }

    public static void stop() {
        if (currentInstance != null) {
            MinecraftClient.getInstance().getSoundManager().stop(currentInstance);
            currentInstance = null;
        }
    }
}
