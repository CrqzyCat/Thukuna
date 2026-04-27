package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public class DoorBlockMixin {

    private static final Identifier HELLO_GOJO_ID = Identifier.of("thukuna", "hello_gojo");
    private static AbstractSoundInstance currentDoorSound = null;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand,
                                  BlockHitResult hitResult,
                                  CallbackInfoReturnable<ActionResult> cir) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = player.getEntityWorld().getBlockState(pos);

        if (!(state.getBlock() instanceof DoorBlock)) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        // Nicht abspielen wenn der letzte Sound noch läuft
        if (currentDoorSound != null &&
            mc.getSoundManager().isPlaying(currentDoorSound)) {
            return;
        }

        SoundEvent event = SoundEvent.of(HELLO_GOJO_ID);
        currentDoorSound = new AbstractSoundInstance(event, SoundCategory.MASTER, Random.create()) {
            @Override public float getVolume() { return 1.0f; }
            @Override public float getPitch()  { return 1.0f; }
            @Override public boolean isRepeatable() { return false; }
        };
        mc.getSoundManager().play(currentDoorSound);
    }
}
