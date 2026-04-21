package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(SoundManager.class)
public class WardenSoundMixin {

    private static final Identifier HOLLOW_PURPLE_ID = Identifier.of("thukuna", "hollow_purple");

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void onPlay(net.minecraft.client.sound.SoundInstance sound, CallbackInfo ci) {
        if (sound.getId() != null &&
            sound.getId().equals(SoundEvents.ENTITY_WARDEN_SONIC_BOOM.value().getId())) {

            ci.cancel();

            SoundEvent hollowPurple = SoundEvent.of(HOLLOW_PURPLE_ID);
            AbstractSoundInstance instance = new AbstractSoundInstance(hollowPurple, SoundCategory.HOSTILE, Random.create()) {
                @Override public float getVolume() { return 1.0f; }
                @Override public float getPitch()  { return 1.0f; }
                @Override public boolean isRepeatable() { return false; }
            };
            // Direkt auf der SoundManager-Instanz aufrufen ohne erneut zu triggern
            MinecraftClient.getInstance().execute(() ->
                MinecraftClient.getInstance().getSoundManager().play(instance)
            );
        }
    }
}
