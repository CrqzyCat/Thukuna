package me.thukunua.thukuna.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Environment(EnvType.CLIENT)
public class ThukunaVideoScreen extends Screen {

    private static final Identifier TEXTURE = Identifier.of("thukuna", "video_frame");

    private NativeImageBackedTexture texture;
    private FFmpegFrameGrabber grabber;
    private Thread videoThread;

    private int videoWidth;
    private int videoHeight;

    private volatile boolean running = true;
    private volatile boolean loaded = false;
    private volatile boolean failed = false;
    private volatile boolean finished = false;

    private volatile int[] latestFramePixels = null;
    private volatile boolean newFrameAvailable = false;

    public ThukunaVideoScreen() {
        super(Text.literal("Thukuna"));
    }

    @Override
    protected void init() {
        loadVideo();
    }

    private void loadVideo() {
        videoThread = new Thread(() -> {
            File tmp = null;
            try {
                InputStream is = ThukunaVideoScreen.class.getResourceAsStream("/assets/thukuna/videos/thukuna.mp4");
                if (is == null) {
                    failed = true;
                    return;
                }

                tmp = File.createTempFile("thukuna", ".mp4");
                tmp.deleteOnExit();

                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    is.transferTo(fos);
                }

                grabber = new FFmpegFrameGrabber(tmp);
                grabber.start();

                videoWidth = grabber.getImageWidth();
                videoHeight = grabber.getImageHeight();

                double fps = grabber.getVideoFrameRate();
                long frameDelay = fps > 0 ? (long) (1000 / fps) : 33;

                Java2DFrameConverter converter = new Java2DFrameConverter();
                Frame frame;

                loaded = true;
                long startTime = System.currentTimeMillis();
                int frameCount = 0;

                while (running && (frame = grabber.grabImage()) != null) {
                    BufferedImage img = converter.convert(frame);
                    if (img != null) {
                        BufferedImage rgba = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_INT_ARGB);
                        rgba.getGraphics().drawImage(img, 0, 0, null);

                        int[] pixels = new int[videoWidth * videoHeight];
                        rgba.getRGB(0, 0, videoWidth, videoHeight, pixels, 0, videoWidth);

                        latestFramePixels = pixels;
                        newFrameAvailable = true;
                    }

                    frameCount++;
                    long expectedTime = startTime + (frameCount * frameDelay);
                    long sleepTime = expectedTime - System.currentTimeMillis();

                    if (sleepTime > 0) Thread.sleep(sleepTime);
                }
                finished = true;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                failed = true;
                e.printStackTrace();
            } finally {
                if (grabber != null) {
                    try { grabber.stop(); grabber.release(); } catch (Exception ignored) {}
                }
                if (tmp != null && tmp.exists()) tmp.delete();
            }
        }, "video-loader");

        videoThread.start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF000000);

        if (failed) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Video Fehler"), width / 2, height / 2, 0xFF5555);
            return;
        }

        if (!loaded) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Loading..."), width / 2, height / 2, 0xFFFFFF);
            return;
        }

        if (finished) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }

        if (texture == null && videoWidth > 0 && videoHeight > 0) {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, videoWidth, videoHeight, false);
            texture = new NativeImageBackedTexture(() -> "thukuna_video", img);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, texture);
        }

        if (newFrameAvailable && latestFramePixels != null && texture != null) {
            updateTexture(latestFramePixels);
            newFrameAvailable = false;
        }

        if (texture != null) {
            float scale = Math.min((float) width / videoWidth, (float) height / videoHeight);
            int drawW = Math.round(videoWidth * scale);
            int drawH = Math.round(videoHeight * scale);
            int xPos = (width - drawW) / 2;
            int yPos = (height - drawH) / 2;

            // FIX: Verwende direkt das RenderLayer-Objekt anstatt einer Methodenreferenz
            // Falls 'getGuiTextured' rot ist, probiere 'getGui'
            context.drawTexture(
                    RenderLayer.getGuiTextured(TEXTURE),
                    TEXTURE,
                    xPos,
                    yPos,
                    0.0f,
                    0.0f,
                    drawW,
                    drawH,
                    videoWidth,
                    videoHeight
            );
        }
    }

    private void updateTexture(int[] pixels) {
        NativeImage img = texture.getImage();
        for (int y = 0; y < videoHeight; y++) {
            for (int x = 0; x < videoWidth; x++) {
                int argb = pixels[y * videoWidth + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                img.setColorArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        texture.upload();
    }

    @Override
    public void removed() {
        super.removed();
        running = false;
        if (videoThread != null) videoThread.interrupt();
        if (this.texture != null) {
            this.texture.close();
            MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}