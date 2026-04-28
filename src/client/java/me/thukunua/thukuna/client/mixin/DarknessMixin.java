package me.thukunua.thukuna.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderPipelines;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class DarknessMixin {

    private static final Identifier VIGNETTE = Identifier.of("thukuna", "textures/infinite_void_vignette.png");

    @Inject(method = "renderVignetteOverlay", at = @At("TAIL"))
    private void onRenderVignetteOverlay(DrawContext context, Entity entity, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!client.player.hasStatusEffect(StatusEffects.DARKNESS)) return;

        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();

        // Lila Vignette Textur ueber den ganzen Screen strecken
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                VIGNETTE,
                0, 0,
                0f, 0f,
                w, h,
                w, h
        );

        // Leichter lila Schleier
        context.fill(0, 0, w, h, 0x28150030);

        // Sterne
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
                int alpha = (int)(200 * Math.min(1.0f, (dist - 0.5f) * 2f));
                context.fill(sx, sy, sx + 1, sy + 1, (alpha << 24) | 0x9900FF);
            }
        }
    }
}
