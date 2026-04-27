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

        // Farben: transparent in der Mitte, lila aussen
        int transparent = 0x00080018;
        int purple      = 0xCC080018; // lila dunkel aussen

        // 4x fillGradient von jedem Rand zur Mitte - sehr effizient
        // Oben
        context.fillGradient(0, 0, w, h / 2, purple, transparent);
        // Unten
        context.fillGradient(0, h / 2, w, h, transparent, purple);
        // Links
        context.fillGradient(0, 0, w / 2, h, purple, transparent);
        // Rechts
        context.fillGradient(w / 2, 0, w, h, transparent, purple);

        // Leichter lila Schleier
        context.fill(0, 0, w, h, 0x25150030);

        // Sterne - nur einmal berechnen, sehr guenstig
        java.util.Random rand = new java.util.Random(42);
        float cx = w / 2f;
        float cy = h / 2f;
        for (int i = 0; i < 60; i++) {
            int sx = rand.nextInt(w);
            int sy = rand.nextInt(h);
            float dx = (sx - cx) / cx;
            float dy = (sy - cy) / cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.5f) {
                int alpha = (int)(180 * Math.min(1.0f, (dist - 0.5f) * 2f));
                context.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0x9900FF);
            }
        }
    }
}
