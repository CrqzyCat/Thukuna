package me.thukunua.thukuna.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

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

        // Texture einmalig anlegen
        if (textureId == -1) {
            textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }

        // Frame-Pixel in ByteBuffer schreiben (RGBA)
        int[] pixels = frames.get(currentFrame);
        ByteBuffer buf = ByteBuffer.allocateDirect(videoWidth * videoHeight * 4)
                .order(ByteOrder.nativeOrder());
        for (int p : pixels) {
            buf.put((byte) ((p >> 16) & 0xFF)); // R
            buf.put((byte) ((p >> 8)  & 0xFF)); // G
            buf.put((byte) ( p        & 0xFF)); // B
            buf.put((byte) ((p >> 24) & 0xFF)); // A
        }
        buf.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                videoWidth, videoHeight, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);

        // Letterbox berechnen
        float scaleX = (float) this.width  / videoWidth;
        float scaleY = (float) this.height / videoHeight;
        float scale  = Math.min(scaleX, scaleY);
        float drawW  = videoWidth  * scale;
        float drawH  = videoHeight * scale;
        float x      = (this.width  - drawW) / 2f;
        float y      = (this.height - drawH) / 2f;

        // Reines OpenGL-Quad zeichnen (kein Minecraft-Shader noetig)
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, this.width, this.height, 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x,        y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + drawW, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + drawW, y + drawH);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x,        y + drawH);
        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
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
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
        frames.clear();
        super.close();
    }
}
