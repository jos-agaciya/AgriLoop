package com.agriloop.util;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * Helper for creating clean, scalable vector icons using Ikonli (Feather Icons & Material Design).
 * Strictly guarantees NO emojis, NO placeholder rectangles, and crisp vector rendering across all platforms.
 */
public class IconHelper {

    /**
     * Creates a vector FontIcon with the specified icon literal and pixel size.
     */
    public static FontIcon createIcon(Ikon iconCode, int size) {
        FontIcon icon = new FontIcon(iconCode != null ? iconCode : Feather.HELP_CIRCLE);
        icon.setIconSize(size);
        if (!icon.getStyleClass().contains("ikonli-font-icon")) {
            icon.getStyleClass().add("ikonli-font-icon");
        }
        icon.getStyleClass().add("app-vector-icon");
        return icon;
    }

    /**
     * Overload specifically for Feather icons.
     */
    public static FontIcon createIcon(Feather featherIcon, int size) {
        return createIcon((Ikon) featherIcon, size);
    }

    /**
     * Creates a vector FontIcon with custom icon code, size, and CSS color paint.
     */
    public static FontIcon createIcon(Ikon iconCode, int size, Paint paint) {
        FontIcon icon = createIcon(iconCode, size);
        if (paint instanceof Color c) {
            icon.setIconColor(c);
        }
        return icon;
    }

    /**
     * Overload for Feather with Paint.
     */
    public static FontIcon createIcon(Feather featherIcon, int size, Paint paint) {
        return createIcon((Ikon) featherIcon, size, paint);
    }

    /**
     * Creates a vector FontIcon with custom CSS style class.
     */
    public static FontIcon createIcon(Ikon iconCode, int size, String styleClass) {
        FontIcon icon = createIcon(iconCode, size);
        if (styleClass != null && !styleClass.isBlank()) {
            icon.getStyleClass().add(styleClass);
        }
        return icon;
    }

    /**
     * Overload for Feather with styleClass.
     */
    public static FontIcon createIcon(Feather featherIcon, int size, String styleClass) {
        return createIcon((Ikon) featherIcon, size, styleClass);
    }
}
