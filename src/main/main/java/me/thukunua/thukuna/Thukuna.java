package me.thukunua.thukuna;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Thukuna implements ModInitializer {

    public static final String MOD_ID = "thukuna";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Thukuna mod initialized!");
    }
}
