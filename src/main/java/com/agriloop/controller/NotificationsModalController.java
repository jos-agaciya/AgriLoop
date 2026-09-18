package com.agriloop.controller;

import com.agriloop.model.Notification;
import com.agriloop.service.NotificationService;
import com.agriloop.util.IconHelper;
import com.agriloop.view.components.EmptyStateCard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Notifications Center modal.
 */
public class NotificationsModalController implements Initializable {

    @FXML private StackPane notifModalIconContainer;
    @FXML private VBox contentContainer;
    @FXML private Button markAllReadButton;
    @FXML private Button clearAllButton;
    @FXML private Button closeButton;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon icon = IconHelper.createIcon(Feather.BELL, 20);
        icon.setStyle("-fx-icon-color: #059669;");
        notifModalIconContainer.getChildren().add(icon);

        FontIcon closeIcon = IconHelper.createIcon(Feather.X, 15);
        closeIcon.setStyle("-fx-icon-color: #64748B;");
        closeButton.setGraphic(closeIcon);
        closeButton.setText("");

        loadNotifications();

        markAllReadButton.setOnAction(e -> {
            NotificationService.getInstance().markAllAsRead();
            loadNotifications();
        });

        clearAllButton.setOnAction(e -> {
            NotificationService.getInstance().clearAll();
            loadNotifications();
        });

        closeButton.setOnAction(e -> MainLayoutController.closeModal());
    }

    private void loadNotifications() {
        contentContainer.getChildren().clear();
        List<Notification> list = NotificationService.getInstance().getNotificationsForCurrentUser();

        if (list.isEmpty()) {
            EmptyStateCard emptyCard = new EmptyStateCard(
                Feather.BELL,
                "No notifications yet",
                "You're completely caught up! New orders, delivery alerts, and trade updates will appear here."
            );
            contentContainer.getChildren().add(emptyCard);
        } else {
            for (Notification notif : list) {
                HBox card = createNotificationCard(notif);
                contentContainer.getChildren().add(card);
            }
        }
    }

    private HBox createNotificationCard(Notification notif) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        row.setStyle("-fx-padding: 12px 16px; -fx-background-radius: 10px; " + 
            (!notif.isRead() ? "-fx-border-color: #A7F3D0; -fx-border-width: 1px; -fx-background-color: #F0FDF4;" : ""));
        HBox.setHgrow(row, Priority.ALWAYS);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: #E2E8F0; -fx-background-radius: 8px; -fx-padding: 8px;");
        
        Feather featherIcon = Feather.BELL;
        String iconColor = "#059669";
        String type = notif.getNotificationType() != null ? notif.getNotificationType().toUpperCase() : "";
        if (type.contains("ORDER")) {
            featherIcon = Feather.SHOPPING_BAG;
            iconColor = "#2563EB";
        } else if (type.contains("DELIVERY")) {
            featherIcon = Feather.TRUCK;
            iconColor = "#D97706";
        }

        FontIcon fontIcon = IconHelper.createIcon(featherIcon, 16);
        fontIcon.setStyle("-fx-icon-color: " + iconColor + ";");
        iconBox.getChildren().add(fontIcon);

        VBox textCol = new VBox(3);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(notif.getTitle());
        titleLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: -fx-color-text-primary;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        String timeStr = notif.getCreatedAt() != null ? notif.getCreatedAt().format(TIME_FMT) : "Just now";
        Label timeLbl = new Label(timeStr);
        timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-color-text-secondary;");

        topRow.getChildren().addAll(titleLbl, timeLbl);

        Label msgLbl = new Label(notif.getMessage());
        msgLbl.setWrapText(true);
        msgLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #475569;");

        textCol.getChildren().addAll(topRow, msgLbl);
        row.getChildren().addAll(iconBox, textCol);

        row.setOnMouseClicked(e -> {
            if (!notif.isRead()) {
                NotificationService.getInstance().markAsRead(notif.getId());
                notif.setRead(true);
                row.setStyle("-fx-padding: 12px 16px; -fx-background-radius: 10px;");
            }
        });

        return row;
    }
}
