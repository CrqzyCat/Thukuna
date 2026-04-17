package me.thukunua.thukuna.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ThukunaVideoScreen extends Screen {

    private int textureId = -1;
    private List<int[]> frames = new ArrayList<>();
    private int currentFrame = 0;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private long lastFrameTime = 0;
    private long frameDurationMs = 33; // ~30fps default
    private boolean loaded = false;

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
                    ThukunaClient.LOGGER.error("Could not find thukuna.mp4 in resources!");
                    return;
                }

                // Temp file nötig für FFmpeg
                java.io.File tmp = java.io.File.createTempFile("thukuna", ".mp4");
                tmp.deleteOnExit();
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                    is.transferTo(fos);
                }

                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tmp);
                grabber.start();

                videoWidth = grabber.getImageWidth();
                videoHeight = grabber.getImageHeight();
                double fps = grabber.getVideoFrameRate();
                if (fps > 0) frameDurationMs = (long)(1000.0 / fps);

                Java2DFrameConverter converter = new Java2DFrameConverter();
                Frame frame;
                while ((frame = grabber.grabImage()) != null) {
                    BufferedImage img = converter.convert(frame);
                    if (img != null) {
                        // RGBA pixel array speichern
                        int[] pixels = new int[videoWidth * videoHeight];
                        img.getRGB(0, 0, videoWidth, videoHeight, pixels, 0, videoWidth);
                        frames.add(pixels);
                    }
                }
                grabber.stop();
                loaded = true;
                lastFrameTime = System.currentTimeMillis();
                ThukunaClient.LOGGER.info("Thukuna: Loaded {} frames ({}x{})", frames.size(), videoWidth, videoHeight);
            } catch (Exception e) {
                ThukunaClient.LOGGER.error("Error loading video", e);
            }
        }, "Thukuna-VideoLoader").start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Schwarzer Hintergrund
        context.fill(0, 0, this.width, this.height, 0xFF000000);

        if (!loaded || frames.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Loading..."), this.width / 2, this.height / 2, 0xFFFFFF);
            return;
        }

        // Nächsten Frame nach Zeit wählen
        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= frameDurationMs) {
            currentFrame++;
            lastFrameTime = now;
        }

        // Video fertig → Screen schließen
        if (currentFrame >= frames.size()) {
            this.client.setScreen(null);
            return;
        }

        // Texture erzeugen / updaten
        if (textureId == -1) {
            textureId = GL11.glGenTextures();
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        int[] pixels = frames.get(currentFrame);
        ByteBuffer buffer = ByteBuffer.allocateDirect(videoWidth * videoHeight * 4);
        for (int p : pixels) {
            buffer.put((byte)((p >> 16) & 0xFF)); // R
            buffer.put((byte)((p >> 8)  & 0xFF)); // G
            buffer.put((byte)( p        & 0xFF)); // B
            buffer.put((byte)((p >> 24) & 0xFF)); // A
        }
        buffer.flip();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
            videoWidth, videoHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Letterbox: Video in Screengröße einpassen
        float scaleX = (float) this.width  / videoWidth;
        float scaleY = (float) this.height / videoHeight;
        float scale  = Math.min(scaleX, scaleY);
        int drawW = (int)(videoWidth  * scale);
        int drawH = (int)(videoHeight * scale);
        int x = (this.width  - drawW) / 2;
        int y = (this.height - drawH) / 2;

        // Direkt per OpenGL zeichnen
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x,      y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x+drawW,y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x+drawW,y+drawH);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x,      y+drawH);
        GL11.glEnd();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // ESC schließt den Screen
    }

    @Override
    public boolean shouldPause() {
        return true; // Spiel pausiert während Video
    }

    @Override
    public void close() {
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
        super.close();
    }
}
