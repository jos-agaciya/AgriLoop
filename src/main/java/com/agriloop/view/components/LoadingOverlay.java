package com.agriloop.view.components;

import com.agriloop.util.AnimationHelper;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Reusable full-container loading overlay with modern spinner.
 */
public class LoadingOverlay extends StackPane {
    private final Label statusLabel;

    public LoadingOverlay(String message) {
        getStyleClass().add("modal-overlay");
        setAlignment(Pos.CENTER);

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 24px 32px; -fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);");

        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);

        statusLabel = new Label(message != null ? message : "Loading...");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-font-size: 14px;");

        box.getChildren().addAll(pi, statusLabel);
        getChildren().add(box);
    }

    public void setMessage(String message) {
        statusLabel.setText(message);
    }

    public void showIn(StackPane container) {
        if (!container.getChildren().contains(this)) {
            container.getChildren().add(this);
            AnimationHelper.fadeIn(this, null, null);
        }
    }

    public void hideFrom(StackPane container) {
        container.getChildren().remove(this);
    }
}
