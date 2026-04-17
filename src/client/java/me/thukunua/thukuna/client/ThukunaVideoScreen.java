package me.thukunua.thukuna.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

@Environment(EnvType.CLIENT)
public class ThukunaVideoScreen extends Screen {

    private DynamicTexture dynamicTexture = null;
    private Identifier textureId = null;
    private final List<int[]> frames = new ArrayList<>();
    private int currentFrame = 0;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private long lastFrameTime = 0;
    private long frameDurationMs = 33;
    private volatile boolean loaded = false;
    private volatile boolean loadFailed = false;

    public ThukunaVideoScreen() {
        super(Text.literal("Thukuna"));
    }

    @Override
    protected void init() {
        super.init();
        loadVideoFrames();
    }

    private void loadVideoFrames() {
        new Thread(() -> {
            try {
                InputStream is = ThukunaVideoScreen.class.getResourceAsStream(
                        "/assets/thukuna/videos/thukuna.mp4"
                );
                if (is == null) {
                    ThukunaClient.LOGGER.error("Thukuna: thukuna.mp4 nicht gefunden!");
                    loadFailed = true;
                    return;
                }

                File tmp = File.createTempFile("thukuna_video", ".mp4");
                tmp.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    is.transferTo(fos);
                }
                is.close();

                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tmp);
                grabber.start();

                videoWidth  = grabber.getImageWidth();
                videoHeight = grabber.getImageHeight();
                double fps  = grabber.getVideoFrameRate();
                if (fps > 0) frameDurationMs = (long) (1000.0 / fps);

                Java2DFrameConverter converter = new Java2DFrameConverter();
                Frame frame;
                while ((frame = grabber.grabImage()) != null) {
                    BufferedImage img = converter.convert(frame);
                    if (img != null) {
                        BufferedImage rgba = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_INT_ARGB);
                        rgba.getGraphics().drawImage(img, 0, 0, null);
                        int[] pixels = new int[videoWidth * videoHeight];
                        rgba.getRGB(0, 0, videoWidth, videoHeight, pixels, 0, videoWidth);
                        frames.add(pixels);
                    }
                }
                grabber.stop();
                tmp.delete();

                ThukunaClient.LOGGER.info("Thukuna: {} Frames geladen ({}x{}, {}ms/frame)",
                        frames.size(), videoWidth, videoHeight, frameDurationMs);
                loaded = true;
                lastFrameTime = System.currentTimeMillis();

            } catch (Exception e) {
                ThukunaClient.LOGGER.error("Thukuna: Fehler beim Laden des Videos", e);
                loadFailed = true;
            }
        }, "Thukuna-VideoLoader").start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Schwarzer Hintergrund
        context.fill(0, 0, this.width, this.height, 0xFF000000);

        if (loadFailed) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Fehler: Video konnte nicht geladen werden!"),
                    this.width / 2, this.height / 2, 0xFFFF5555);
            return;
        }

        if (!loaded || frames.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Loading..."),
                    this.width / 2, this.height / 2, 0xFFFFFF);
            return;
        }

        // Frame-Timing
        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= frameDurationMs) {
            currentFrame++;
            lastFrameTime = now;
        }

        // Video fertig -> Screen schliessen
        if (currentFrame >= frames.size()) {
            this.client.setScreen(null);
            return;
        }

        // DynamicTexture beim ersten Frame anlegen
        if (dynamicTexture == null) {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, videoWidth, videoHeight, false);
            dynamicTexture = new DynamicTexture(img);
            textureId = MinecraftClient.getInstance().getTextureManager()
                    .registerDynamicTexture("thukuna_video", dynamicTexture);
        }

        // Aktuellen Frame in NativeImage schreiben
        NativeImage img = dynamicTexture.getImage();
        if (img != null) {
            int[] pixels = frames.get(currentFrame);
            for (int y = 0; y < videoHeight; y++) {
                for (int x = 0; x < videoWidth; x++) {
                    int argb = pixels[y * videoWidth + x];
                    // NativeImage erwartet ABGR
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8)  & 0xFF;
                    int b =  argb        & 0xFF;
                    img.setColorArgb(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            dynamicTexture.upload();
        }

        // Letterbox berechnen
        float scaleX = (float) this.width  / videoWidth;
        float scaleY = (float) this.height / videoHeight;
        float scale  = Math.min(scaleX, scaleY);
        int drawW = (int) (videoWidth  * scale);
        int drawH = (int) (videoHeight * scale);
        int x = (this.width  - drawW) / 2;
        int y = (this.height - drawH) / 2;

        // Mit context.drawTexture zeichnen - das ist die sichere Minecraft-API
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.drawTexture(
                net.minecraft.client.gl.ShaderProgramKeys.POSITION_TEX,
                textureId,
                x, y, 0, 0,
                drawW, drawH,
                drawW, drawH
        );
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void close() {
        if (textureId != null && client != null) {
            client.getTextureManager().destroyTexture(textureId);
            textureId = null;
            dynamicTexture = null;
        }
        frames.clear();
        super.close();
    }
}
