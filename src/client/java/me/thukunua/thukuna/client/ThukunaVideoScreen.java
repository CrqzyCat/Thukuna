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
                if (is == null) { failed = true; return; }

                tmp = File.createTempFile("thukuna", ".mp4");
                tmp.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    is.transferTo(fos);
                }

                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tmp);
                grabber.start();

                videoWidth  = grabber.getImageWidth();
                videoHeight = grabber.getImageHeight();
                double fps  = grabber.getVideoFrameRate();
                long frameDelay = fps > 0 ? (long)(1000.0 / fps) : 33;

                Java2DFrameConverter converter = new Java2DFrameConverter();
                loaded = true;

                // Sound starten sobald Video bereit ist
                MinecraftClient.getInstance().execute(VideoAudioPlayer::start);

                long startTime = System.currentTimeMillis();
                int frameCount = 0;
                Frame frame;

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
                    long sleepTime = (startTime + frameCount * frameDelay) - System.currentTimeMillis();
                    if (sleepTime > 0) Thread.sleep(sleepTime);
                }

                grabber.stop();
                grabber.release();
                finished = true;

            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                ThukunaClient.LOGGER.error("Thukuna: Video Fehler", e);
                failed = true;
            } finally {
                if (tmp != null) tmp.delete();
            }
        }, "thukuna-video");
        videoThread.start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF000000);

        if (failed) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Video Fehler!"), width / 2, height / 2, 0xFFFF5555);
            return;
        }

        if (!loaded) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Loading..."), width / 2, height / 2, 0xFFFFFF);
            return;
        }

        if (finished) {
            MinecraftClient.getInstance().setScreen(null);
            return;
        }

        // Texture einmalig anlegen - korrekter Konstruktor fuer 1.21.11
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
            int drawW = Math.round(videoWidth  * scale);
            int drawH = Math.round(videoHeight * scale);
            int xPos  = (width  - drawW) / 2;
            int yPos  = (height - drawH) / 2;

            // Korrekte drawTexture-Signatur fuer 1.21.11
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    TEXTURE,
                    xPos, yPos,
                    0f, 0f,
                    drawW, drawH,
                    drawW, drawH
            );
        }
    }

    private void updateTexture(int[] pixels) {
        NativeImage img = texture.getImage();
        if (img == null) return;
        for (int y = 0; y < videoHeight; y++) {
            for (int x = 0; x < videoWidth; x++) {
                int argb = pixels[y * videoWidth + x];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8)  & 0xFF;
                int b =  argb        & 0xFF;
                img.setColorArgb(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        texture.upload();
    }

    @Override
    public void removed() {
        running = false;
        if (videoThread != null) videoThread.interrupt();
        VideoAudioPlayer.stop();
        if (texture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE);
            texture.close();
            texture = null;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean shouldPause() { return true; }
}
