package me.thukunua.thukuna.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

@Environment(EnvType.CLIENT)
public class ThukunaVideoScreen extends Screen {

    private int textureId = -1;
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

                // FFmpeg braucht eine echte Datei
                File tmp = File.createTempFile("thukuna_video", ".mp4");
                tmp.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    is.transferTo(fos);
                }
                is.close();

                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tmp);
                grabber.start();

                videoWidth = grabber.getImageWidth();
                videoHeight = grabber.getImageHeight();
                double fps = grabber.getVideoFrameRate();
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

        // OpenGL Texture erstellen (einmalig)
        if (textureId == -1) {
            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }

        // Aktuellen Frame in Texture hochladen
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        int[] pixels = frames.get(currentFrame);
        ByteBuffer buffer = ByteBuffer.allocateDirect(videoWidth * videoHeight * 4)
                .order(ByteOrder.nativeOrder());
        for (int p : pixels) {
            buffer.put((byte) ((p >> 16) & 0xFF)); // R
            buffer.put((byte) ((p >> 8)  & 0xFF)); // G
            buffer.put((byte) ( p        & 0xFF)); // B
            buffer.put((byte) ((p >> 24) & 0xFF)); // A
        }
        buffer.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                videoWidth, videoHeight, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Letterbox-Berechnung
        float scaleX = (float) this.width  / videoWidth;
        float scaleY = (float) this.height / videoHeight;
        float scale  = Math.min(scaleX, scaleY);
        int drawW = (int) (videoWidth  * scale);
        int drawH = (int) (videoHeight * scale);
        int x = (this.width  - drawW) / 2;
        int y = (this.height - drawH) / 2;

        // Mit Minecraft's RenderSystem zeichnen
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.bindTexture(textureId);
        RenderSystem.enableBlend();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(x,         y,         0).texture(0f, 0f);
        bufferBuilder.vertex(x,         y + drawH, 0).texture(0f, 1f);
        bufferBuilder.vertex(x + drawW, y + drawH, 0).texture(1f, 1f);
        bufferBuilder.vertex(x + drawW, y,         0).texture(1f, 0f);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.disableBlend();
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
        if (textureId != -1) {
            RenderSystem.assertOnRenderThread();
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
        frames.clear();
        super.close();
    }
}
