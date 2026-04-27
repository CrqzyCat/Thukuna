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

        // Lila Schleier ueber den gesamten Screen
        context.fill(0, 0, w, h, 0x33150030);

        // Gradient Vignette - Zeile fuer Zeile von oben und unten
        int steps = 80;
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            int alpha = (int)(160 * t * t); // quadratischer Verlauf
            int color = (alpha << 24) | 0x080018;

            // Oben
            context.fill(0, i * h / (steps * 2), w, (i + 1) * h / (steps * 2), color);
            // Unten
            context.fill(0, h - (i + 1) * h / (steps * 2), w, h - i * h / (steps * 2), color);
            // Links
            context.fill(i * w / (steps * 2), 0, (i + 1) * w / (steps * 2), h, color);
            // Rechts
            context.fill(w - (i + 1) * w / (steps * 2), 0, w - i * w / (steps * 2), h, color);
        }

        // Sterne
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 80; i++) {
            int sx = rand.nextInt(w);
            int sy = rand.nextInt(h);
            float dx = (sx - cx) / (float) cx;
            float dy = (sy - cy) / (float) cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.5f) {
                int alpha = (int)(200 * Math.min(1.0f, (dist - 0.5f) * 2f));
                context.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0x9900FF);
            }
        }
    }
}
