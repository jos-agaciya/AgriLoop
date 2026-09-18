package com.agriloop.util;

import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * Manages loading and registering Google Sans font family globally in JavaFX.
 */
public class FontManager {
    private static final Logger logger = LoggerFactory.getLogger(FontManager.class);
    private static boolean fontsLoaded = false;

    public static final String FONT_FAMILY = "Google Sans";

    /**
     * Loads all packaged Google Sans font variants.
     */
    public static synchronized void loadFonts() {
        if (fontsLoaded) return;

        String[] fontFiles = {
            "GoogleSans-Regular.ttf",
            "GoogleSans-Medium.ttf",
            "GoogleSans-Bold.ttf",
            "GoogleSans-Italic.ttf"
        };

        for (String fontFile : fontFiles) {
            String path = "/fonts/" + fontFile;
            try (InputStream in = FontManager.class.getResourceAsStream(path)) {
                if (in != null) {
                    Font font = Font.loadFont(in, 14);
                    if (font != null) {
                        logger.info("Loaded font: {} (Family: {})", font.getName(), font.getFamily());
                    } else {
                        logger.warn("Failed to load font from {}", path);
                    }
                } else {
                    logger.warn("Font file not found: {}", path);
                }
            } catch (Exception e) {
                logger.error("Error loading font: " + path, e);
            }
        }

        fontsLoaded = true;
    }
}
