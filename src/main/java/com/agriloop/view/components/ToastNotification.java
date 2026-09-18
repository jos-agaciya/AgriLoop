package com.agriloop.view.components;

import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Toast alert notification that animates in and dismisses automatically.
 */
public class ToastNotification extends HBox {

    public enum Type {
        SUCCESS("#ECFDF5", "#059669", Feather.CHECK_CIRCLE),
        ERROR("#FEF2F2", "#DC2626", Feather.ALERT_CIRCLE),
        INFO("#EFF6FF", "#2563EB", Feather.INFO),
        WARNING("#FFFBEB", "#D97706", Feather.ALERT_TRIANGLE);

        public final String bgHex;
        public final String textHex;
        public final Feather icon;

        Type(String bgHex, String textHex, Feather icon) {
            this.bgHex = bgHex;
            this.textHex = textHex;
            this.icon = icon;
        }
    }

    public ToastNotification(String message, Type type) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 10px; -fx-padding: 12px 18px; " +
            "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.15), 14, 0, 0, 4); " +
            "-fx-border-color: %s; -fx-border-radius: 10px; -fx-border-width: 1px;",
            type.bgHex, type.textHex
        ));

        FontIcon icon = IconHelper.createIcon(type.icon, 18);
        icon.setStyle("-fx-icon-color: " + type.textHex + ";");

        Label label = new Label(message);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + type.textHex + "; -fx-font-size: 13px;");

        getChildren().addAll(icon, label);
    }

    public static void show(Pane parent, String message, Type type) {
        ToastNotification toast = new ToastNotification(message, type);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        toast.setTranslateY(20);
        toast.setTranslateX(-20);

        parent.getChildren().add(toast);
        AnimationHelper.fadeSlideIn(toast, -15, Duration.millis(200));

        PauseTransition pause = new PauseTransition(Duration.seconds(3.5));
        pause.setOnFinished(e -> {
            parent.getChildren().remove(toast);
        });
        pause.play();
    }
}
