package com.agriloop.controller;

import com.agriloop.model.User;
import com.agriloop.service.NavigationService;
import com.agriloop.service.NotificationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.IconHelper;
import com.agriloop.view.ViewType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Top Bar header component.
 */
public class TopBarController implements Initializable {

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private StackPane searchIconContainer;
    @FXML private TextField searchInputField;
    @FXML private Button notificationsButton;
    @FXML private StackPane notifIconContainer;
    @FXML private Label unreadBadgeLabel;
    @FXML private HBox profileChip;
    @FXML private StackPane avatarIconContainer;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Search Icon
        FontIcon searchIcon = IconHelper.createIcon(Feather.SEARCH, 15);
        searchIcon.setStyle("-fx-icon-color: #94A3B8;");
        searchIconContainer.getChildren().add(searchIcon);

        // Notification Bell Icon
        FontIcon bellIcon = IconHelper.createIcon(Feather.BELL, 16);
        bellIcon.setStyle("-fx-icon-color: #475569;");
        notifIconContainer.getChildren().add(bellIcon);

        // Avatar Icon
        FontIcon userIcon = IconHelper.createIcon(Feather.USER, 16);
        userIcon.setStyle("-fx-icon-color: #059669;");
        avatarIconContainer.getChildren().add(userIcon);

        // Navigation update listener
        NavigationService.getInstance().addNavigationListener(viewType -> Platform.runLater(() -> {
            pageTitleLabel.setText(viewType.getTitle());
            pageSubtitleLabel.setText(viewType.getSubtitle());
        }));

        // Notification listener
        NotificationService.getInstance().addListener(list -> Platform.runLater(() -> {
            int unread = NotificationService.getInstance().getUnreadCount();
            if (unread > 0) {
                unreadBadgeLabel.setText(String.valueOf(unread));
                unreadBadgeLabel.setVisible(true);
            } else {
                unreadBadgeLabel.setVisible(false);
            }
        }));

        // Profile session status update
        updateProfileDisplay(ProfileService.getInstance().getCurrentUser());
        ProfileService.getInstance().setOnUserChangeListener(this::updateProfileDisplay);

        // Handlers
        notificationsButton.setOnAction(e -> MainLayoutController.openNotificationsModal());
        profileChip.setOnMouseClicked(e -> NavigationService.getInstance().navigateTo(ViewType.PROFILE));

        // Global Search Handlers
        searchInputField.setOnAction(e -> handleGlobalSearch());
        searchIconContainer.setOnMouseClicked(e -> handleGlobalSearch());
        searchIconContainer.setStyle("-fx-cursor: hand;");
    }

    private void handleGlobalSearch() {
        String query = searchInputField.getText() != null ? searchInputField.getText().trim() : "";
        if (query.isEmpty()) return;

        User current = ProfileService.getInstance().getCurrentUser();
        if (current != null && current.getRole() == com.agriloop.model.enums.UserRole.TRANSPORTER) {
            NavigationService.getInstance().navigateTo(ViewType.DELIVERY_REQUESTS);
        } else {
            NavigationService.getInstance().navigateToWithSearch(ViewType.MARKETPLACE, query);
        }
    }

    private void updateProfileDisplay(User user) {
        Platform.runLater(() -> {
            if (user != null) {
                profileNameLabel.setText(user.getFullName());
                profileRoleLabel.setText(user.getRole().getDisplayName());
            } else {
                profileNameLabel.setText("Account");
                profileRoleLabel.setText("Member");
            }
        });
    }
}
