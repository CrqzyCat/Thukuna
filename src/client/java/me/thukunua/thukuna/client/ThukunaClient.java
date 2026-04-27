package me.thukunua.thukuna.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class ThukunaClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("thukuna");
    public static boolean shouldShowVideo = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Thukuna Client initialized!");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shouldShowVideo && client.player != null) {
                shouldShowVideo = false;
                // Nur nicht oeffnen wenn bereits ein Video laeuft
                if (!(client.currentScreen instanceof ThukunaVideoScreen)) {
                    MinecraftClient.getInstance().execute(() ->
                        MinecraftClient.getInstance().setScreen(new ThukunaVideoScreen())
                    );
                }
            }
        });
    }
}
