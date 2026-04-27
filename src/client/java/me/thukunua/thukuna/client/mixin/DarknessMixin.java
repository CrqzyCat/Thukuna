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
        float cx = w / 2f;
        float cy = h / 2f;
        float maxDist = (float) Math.sqrt(cx * cx + cy * cy);

        // Leichter lila Schleier ueber alles
        context.fill(0, 0, w, h, 0x22100025);

        // Radialer Gradient - pixel fuer pixel (in groesseren Bloecken fuer Performance)
        int blockSize = 4;
        for (int y = 0; y < h; y += blockSize) {
            for (int x = 0; x < w; x += blockSize) {
                float dx = x - cx;
                float dy = y - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy) / maxDist;

                // Nur am Rand dunkel, Mitte transparent
                if (dist > 0.35f) {
                    float t = (dist - 0.35f) / 0.65f;
                    t = t * t; // quadratisch fuer weicheren Verlauf
                    int alpha = (int)(180 * t);
                    context.fill(x, y, Math.min(x + blockSize, w), Math.min(y + blockSize, h),
                            (alpha << 24) | 0x080018);
                }
            }
        }

        // Sterne
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 80; i++) {
            int sx = rand.nextInt(w);
            int sy = rand.nextInt(h);
            float dx = (sx - cx) / cx;
            float dy = (sy - cy) / cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.5f) {
                int alpha = (int)(200 * Math.min(1.0f, (dist - 0.5f) * 2f));
                context.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0x9900FF);
            }
        }
    }
}
