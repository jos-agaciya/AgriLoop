package com.agriloop.view.components;

import com.agriloop.util.IconHelper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable, modern Empty State component for views with zero database records.
 * 100% Emoji-free, uses crisp Ikonli vector iconography.
 */
public class EmptyStateCard extends VBox {

    public EmptyStateCard(Feather icon, String title, String description) {
        this(icon, title, description, null, null);
    }

    public EmptyStateCard(Feather icon, String title, String description, String actionButtonText, Runnable onAction) {
        setAlignment(Pos.CENTER);
        setSpacing(14);
        getStyleClass().add("empty-state-card");

        // Circular Icon Badge
        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("empty-state-icon-circle");
        FontIcon fontIcon = IconHelper.createIcon(icon != null ? icon : Feather.INBOX, 32);
        fontIcon.setStyle("-fx-icon-color: #64748B;");
        iconCircle.getChildren().add(fontIcon);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-state-title");

        // Description
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("empty-state-description");
        descLabel.setWrapText(true);

        getChildren().addAll(iconCircle, titleLabel, descLabel);

        // Optional Action Button
        if (actionButtonText != null && !actionButtonText.isBlank() && onAction != null) {
            Button actionBtn = new Button(actionButtonText);
            actionBtn.getStyleClass().addAll("btn-primary");
            actionBtn.setOnAction(e -> onAction.run());
            getChildren().add(actionBtn);
        }
    }
}
