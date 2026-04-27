package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class DarknessMixin {

    @Inject(method = "renderDarkness", at = @At("HEAD"), cancellable = true)
    private void onRenderDarkness(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!client.player.hasStatusEffect(StatusEffects.DARKNESS)) return;

        // Darkness rendering abbrechen und durch lila Infinite Void ersetzen
        ci.cancel();

        int screenWidth  = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Lila Vignette von den Raendern her
        // Mehrere halb-transparente lila Schichten fuer Tiefeneffekt
        int cx = screenWidth  / 2;
        int cy = screenHeight / 2;

        // Aeussere dunkle lila Schicht
        drawVignette(context, screenWidth, screenHeight, 0x99080018);
        // Mittlere lila Schicht
        drawVignette(context, screenWidth, screenHeight, 0x66200040);

        // Sterne - kleine helle lila Punkte
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 80; i++) {
            int sx = rand.nextInt(screenWidth);
            int sy = rand.nextInt(screenHeight);
            // Nur am Rand sichtbar
            float dx = (sx - cx) / (float) cx;
            float dy = (sy - cy) / (float) cy;
            float dist = (float) Math.sqrt(dx*dx + dy*dy);
            if (dist > 0.4f) {
                int alpha = (int)(150 * Math.min(1.0f, dist));
                context.fill(sx, sy, sx+1, sy+1, (alpha << 24) | 0x9900FF);
            }
        }
    }

    private void drawVignette(DrawContext context, int w, int h, int color) {
        // Oberer Bereich
        context.fill(0, 0,   w, h/4, color);
        // Unterer Bereich
        context.fill(0, h*3/4, w, h, color);
        // Linker Bereich
        context.fill(0, 0,   w/4, h, color);
        // Rechter Bereich
        context.fill(w*3/4, 0, w, h, color);
    }
}
