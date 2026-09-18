package com.agriloop.view.components;

import com.agriloop.controller.MainLayoutController;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable, compact, centered modal dialog for AgriLoop.
 * Renders cleanly over the active page with bounded dimensions, subtle shadow,
 * rounded corners, clear action buttons, and keyboard escape dismissal.
 */
public class CompactModal {

    private static final String MODAL_OVERLAY_ID = "agriloop-active-compact-modal";

    public enum Type {
        SUCCESS(Feather.CHECK_CIRCLE, "#059669", "#ECFDF5", "btn-primary"),
        ERROR(Feather.ALERT_CIRCLE, "#DC2626", "#FEF2F2", "btn-danger"),
        INFO(Feather.INFO, "#2563EB", "#EFF6FF", "btn-primary"),
        WARNING(Feather.ALERT_TRIANGLE, "#D97706", "#FFFBEB", "btn-primary"),
        CONFIRMATION(Feather.HELP_CIRCLE, "#059669", "#ECFDF5", "btn-primary");

        public final Feather icon;
        public final String accentColor;
        public final String iconBgColor;
        public final String primaryBtnClass;

        Type(Feather icon, String accentColor, String iconBgColor, String primaryBtnClass) {
            this.icon = icon;
            this.accentColor = accentColor;
            this.iconBgColor = iconBgColor;
            this.primaryBtnClass = primaryBtnClass;
        }
    }

    /**
     * Displays a compact success modal dialog.
     */
    public static void showSuccess(Pane root, String title, String message, Runnable onClose) {
        show(root, Type.SUCCESS, title, message, "Close", onClose, null, null);
    }

    /**
     * Displays a compact error modal dialog.
     */
    public static void showError(Pane root, String title, String message, Runnable onClose) {
        show(root, Type.ERROR, title, message, "Dismiss", onClose, null, null);
    }

    /**
     * Displays a compact informational modal dialog.
     */
    public static void showInfo(Pane root, String title, String message, Runnable onClose) {
        show(root, Type.INFO, title, message, "OK", onClose, null, null);
    }

    /**
     * Displays a compact confirmation modal dialog with Cancel and Confirm buttons.
     */
    public static void showConfirmation(Pane root, String title, String message, String confirmBtnText, Runnable onConfirm, Runnable onCancel) {
        show(root, Type.CONFIRMATION, title, message, confirmBtnText != null ? confirmBtnText : "Confirm", onConfirm, "Cancel", onCancel);
    }

    private static Pane resolveRootPane(Pane root) {
        if (root != null) return root;
        if (MainLayoutController.getInstance() != null && MainLayoutController.getInstance().getRootStackPane() != null) {
            return MainLayoutController.getInstance().getRootStackPane();
        }
        return null;
    }

    private static void show(
            Pane root,
            Type type,
            String title,
            String message,
            String primaryBtnText,
            Runnable onPrimaryAction,
            String secondaryBtnText,
            Runnable onSecondaryAction
    ) {
        final Pane targetRoot = resolveRootPane(root);
        if (targetRoot == null) return;

        // Dismiss any existing active modal first to prevent duplicates
        closeActiveModal(targetRoot);

        // Overlay Backdrop
        StackPane overlay = new StackPane();
        overlay.setId(MODAL_OVERLAY_ID);
        overlay.setStyle("-fx-background-color: rgba(15, 23, 42, 0.42);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(20));

        // Dialog Card Container
        VBox card = new VBox(14);
        card.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 12px; " +
            "-fx-padding: 20px 22px; " +
            "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.16), 20, 0, 0, 6); " +
            "-fx-max-width: 440px; " +
            "-fx-min-width: 360px; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-width: 1px;"
        );
        card.setAlignment(Pos.TOP_LEFT);

        // Header Row (Icon + Title + Spacer + Close X)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 8px; -fx-padding: 7px;", type.iconBgColor));
        FontIcon icon = IconHelper.createIcon(type.icon, 18);
        icon.setStyle("-fx-icon-color: " + type.accentColor + ";");
        iconBox.getChildren().add(icon);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15.5px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeXBtn = new Button();
        FontIcon xIcon = IconHelper.createIcon(Feather.X, 15);
        xIcon.setStyle("-fx-icon-color: #94A3B8;");
        closeXBtn.setGraphic(xIcon);
        closeXBtn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 4px 6px; " +
            "-fx-background-radius: 6px;"
        );
        closeXBtn.setOnMouseEntered(e -> {
            closeXBtn.setStyle("-fx-background-color: #F1F5F9; -fx-cursor: hand; -fx-padding: 4px 6px; -fx-background-radius: 6px;");
            xIcon.setStyle("-fx-icon-color: #334155;");
        });
        closeXBtn.setOnMouseExited(e -> {
            closeXBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4px 6px; -fx-background-radius: 6px;");
            xIcon.setStyle("-fx-icon-color: #94A3B8;");
        });

        header.getChildren().addAll(iconBox, titleLabel, spacer, closeXBtn);

        // Body Message
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #475569; -fx-line-spacing: 2px;");
        messageLabel.setMaxWidth(390);

        // Footer Actions
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding: 6px 0 0 0;");

        Runnable dismissAction = () -> {
            targetRoot.getChildren().remove(overlay);
            closeActiveModal(targetRoot);
        };

        closeXBtn.setOnAction(e -> {
            dismissAction.run();
            if (onSecondaryAction != null) {
                onSecondaryAction.run();
            }
        });

        if (secondaryBtnText != null) {
            Button secBtn = new Button(secondaryBtnText);
            secBtn.getStyleClass().add("btn-secondary");
            secBtn.setStyle("-fx-padding: 7px 15px; -fx-font-size: 12.5px;");
            secBtn.setOnAction(e -> {
                dismissAction.run();
                if (onSecondaryAction != null) {
                    onSecondaryAction.run();
                }
            });
            footer.getChildren().add(secBtn);
        }

        Button primBtn = new Button(primaryBtnText != null ? primaryBtnText : "Close");
        primBtn.getStyleClass().add(type.primaryBtnClass);
        primBtn.setStyle("-fx-padding: 7px 18px; -fx-font-size: 12.5px; -fx-font-weight: bold;");
        primBtn.setOnAction(e -> {
            dismissAction.run();
            if (onPrimaryAction != null) {
                onPrimaryAction.run();
            }
        });
        footer.getChildren().add(primBtn);

        card.getChildren().addAll(header, messageLabel, footer);
        overlay.getChildren().add(card);

        // Escape Key Support
        overlay.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                dismissAction.run();
                if (onSecondaryAction != null) {
                    onSecondaryAction.run();
                }
            }
        });

        // Add to Root Container
        targetRoot.getChildren().add(overlay);
        overlay.requestFocus();
        AnimationHelper.fadeIn(overlay, null, null);
    }

    public static void closeActiveModal(Pane root) {
        if (root == null) return;
        java.util.List<Node> toRemove = new java.util.ArrayList<>();
        for (Node child : root.getChildren()) {
            if (MODAL_OVERLAY_ID.equals(child.getId())) {
                toRemove.add(child);
            }
        }
        root.getChildren().removeAll(toRemove);
    }
}
