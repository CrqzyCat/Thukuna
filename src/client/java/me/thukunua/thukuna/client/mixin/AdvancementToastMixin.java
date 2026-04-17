package me.thukunua.thukuna.client.mixin;

import me.thukunua.thukuna.client.ThukunaClient;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementToast.class)
public class AdvancementToastMixin {

    @Inject(method = "show", at = @At("HEAD"))
    private void onAdvancementToastShow(ToastManager manager, CallbackInfo ci) {
        ThukunaClient.LOGGER.info("Thukuna: Advancement bekommen! Video wird abgespielt...");
        ThukunaClient.shouldShowVideo = true;
    }
}
