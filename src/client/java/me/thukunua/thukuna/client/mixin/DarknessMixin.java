package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class DarknessMixin {

    @Inject(method = "renderVignetteOverlay", at = @At("TAIL"))
    private void onRenderVignetteOverlay(DrawContext context, Entity entity, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!client.player.hasStatusEffect(StatusEffects.DARKNESS)) return;

        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        int cx = w / 2;
        int cy = h / 2;

        // Lila Vignette von den Raendern
        context.fill(0,     0,     w,     h/3,   0x88080018);
        context.fill(0,     h*2/3, w,     h,     0x88080018);
        context.fill(0,     0,     w/3,   h,     0x88080018);
        context.fill(w*2/3, 0,     w,     h,     0x88080018);

        // Lila Schleier ueber alles
        context.fill(0, 0, w, h, 0x33150030);

        // Sterne
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 80; i++) {
            int sx = rand.nextInt(w);
            int sy = rand.nextInt(h);
            float dx = (sx - cx) / (float) cx;
            float dy = (sy - cy) / (float) cy;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            if (dist > 0.4f) {
                int alpha = (int)(180 * Math.min(1.0f, dist));
                context.fill(sx, sy, sx+1, sy+1, (alpha << 24) | 0x9900FF);
            }
        }
    }
}
