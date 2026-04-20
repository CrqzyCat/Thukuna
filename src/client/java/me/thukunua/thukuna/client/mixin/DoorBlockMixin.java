package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(DoorBlock.class)
public class DoorBlockMixin {

    private static final Identifier HELLO_GOJO_ID = Identifier.of("thukuna", "hello_gojo");

    @Inject(method = "onUse", at = @At("HEAD"))
    private void onDoorOpen(BlockState state, World world, net.minecraft.util.math.BlockPos pos,
                            PlayerEntity player, BlockHitResult hit,
                            CallbackInfoReturnable<ActionResult> cir) {
        // Nur clientseitig und nur beim unteren Teil der Tür (sonst wird es 2x gefeuert)
        if (!world.isClient) return;
        if (state.get(Properties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) return;

        SoundEvent event = SoundEvent.of(HELLO_GOJO_ID);
        AbstractSoundInstance instance = new AbstractSoundInstance(event, SoundCategory.MASTER, Random.create()) {
            @Override public float getVolume() { return 1.0f; }
            @Override public float getPitch()  { return 1.0f; }
            @Override public boolean isRepeatable() { return false; }
        };
        MinecraftClient.getInstance().getSoundManager().play(instance);
    }
}
