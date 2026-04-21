package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WardenEntity.class)
public class WardenSoundMixin {

    private static final Identifier HOLLOW_PURPLE_ID = Identifier.of("thukuna", "hollow_purple");

    @Inject(
        method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        // Nur clientseitig
        if (!((WardenEntity)(Object)this).getWorld().isClient) return;

        // Nur den Sonic Boom Sound ersetzen
        if (sound == SoundEvents.ENTITY_WARDEN_SONIC_BOOM) {
            ci.cancel(); // Originalen Sound unterdrücken

            SoundEvent hollowPurple = SoundEvent.of(HOLLOW_PURPLE_ID);
            AbstractSoundInstance instance = new AbstractSoundInstance(hollowPurple, SoundCategory.HOSTILE, Random.create()) {
                @Override public float getVolume() { return 1.0f; }
                @Override public float getPitch()  { return 1.0f; }
                @Override public boolean isRepeatable() { return false; }
            };
            MinecraftClient.getInstance().getSoundManager().play(instance);
        }
    }
}
