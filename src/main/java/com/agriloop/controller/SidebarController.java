package com.agriloop.controller;

import com.agriloop.App;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.service.NavigationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.IconHelper;
import com.agriloop.view.ViewType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the role-isolated Sidebar navigation.
 * Dynamically populated according to the authenticated user's assigned role.
 */
public class SidebarController implements Initializable {

    @FXML private VBox sidebarContainer;
    @FXML private StackPane logoIconContainer;
    @FXML private StackPane userAvatarIconBox;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleBadgeLabel;
    @FXML private VBox navButtonsContainer;
    @FXML private Button signOutButton;
    @FXML private StackPane signOutIconBox;

    private final Map<ViewType, Button> navButtonsMap = new EnumMap<>(ViewType.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Official AgriLoop Brand Logo
        try {
            javafx.scene.image.Image logoImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/images/agriloop-app-icon.png"), 36, 36, true, true);
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logoImg);
            logoView.setFitWidth(36);
            logoView.setFitHeight(36);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(36, 36);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            logoView.setClip(clip);

            logoIconContainer.getChildren().clear();
            logoIconContainer.getChildren().add(logoView);
            logoIconContainer.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-alignment: CENTER;");
        } catch (Exception e) {
            FontIcon brandIcon = IconHelper.createIcon(Feather.PACKAGE, 20);
            brandIcon.setStyle("-fx-icon-color: #FFFFFF;");
            logoIconContainer.getChildren().add(brandIcon);
        }

        // User Avatar Icon
        FontIcon userIcon = IconHelper.createIcon(Feather.USER, 18);
        userIcon.setStyle("-fx-icon-color: #059669;");
        userAvatarIconBox.getChildren().add(userIcon);

        // Sign Out Icon
        FontIcon signOutIcon = IconHelper.createIcon(Feather.LOG_OUT, 16);
        signOutIcon.setStyle("-fx-icon-color: #94A3B8;");
        signOutIconBox.getChildren().add(signOutIcon);

        // Update User Identity & Role Navigation
        updateUserIdentity(ProfileService.getInstance().getCurrentUser());
        ProfileService.getInstance().setOnUserChangeListener(this::updateUserIdentity);

        // Register Navigation Listener
        NavigationService.getInstance().addNavigationListener(this::highlightActiveNavButton);

        // Sign Out Action
        signOutButton.setOnAction(e -> App.showAuthScreen());
    }

    private void updateUserIdentity(User user) {
        Platform.runLater(() -> {
            if (user != null) {
                userNameLabel.setText(user.getFullName());
                userRoleBadgeLabel.setText(user.getRole().getDisplayName().toUpperCase());
                buildRoleNavigation(user.getRole());
            } else {
                userNameLabel.setText("User");
                userRoleBadgeLabel.setText("MEMBER");
                buildRoleNavigation(UserRole.FARMER);
            }
        });
    }

    private void buildRoleNavigation(UserRole role) {
        navButtonsContainer.getChildren().clear();
        navButtonsMap.clear();

        ViewType[] routes = switch (role) {
            case FARMER -> new ViewType[]{
                ViewType.DASHBOARD,
                ViewType.SELL_WASTE,
                ViewType.MY_LISTINGS,
                ViewType.SALES_REQUESTS,
                ViewType.IMPACT,
                ViewType.PROFILE
            };
            case MANUFACTURER -> new ViewType[]{
                ViewType.DASHBOARD,
                ViewType.MARKETPLACE,
                ViewType.ORDERS,
                ViewType.DELIVERIES,
                ViewType.IMPACT,
                ViewType.PROFILE
            };
            case TRANSPORTER -> new ViewType[]{
                ViewType.DASHBOARD,
                ViewType.DELIVERY_REQUESTS,
                ViewType.ACTIVE_DELIVERIES,
                ViewType.EARNINGS,
                ViewType.PROFILE
            };
            case ADMIN -> new ViewType[]{
                ViewType.DASHBOARD,
                ViewType.MARKETPLACE,
                ViewType.MY_LISTINGS,
                ViewType.ORDERS,
                ViewType.DELIVERIES,
                ViewType.EARNINGS,
                ViewType.IMPACT,
                ViewType.PROFILE
            };
        };

        for (ViewType route : routes) {
            Button btn = createNavButton(route);
            navButtonsMap.put(route, btn);
            navButtonsContainer.getChildren().add(btn);
        }

        highlightActiveNavButton(NavigationService.getInstance().getCurrentView());
    }

    private Button createNavButton(ViewType route) {
        Button btn = new Button(route.getTitle());
        btn.getStyleClass().add("nav-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        FontIcon icon = IconHelper.createIcon(route.getIcon(), 17);
        icon.setStyle("-fx-icon-color: #94A3B8;");
        btn.setGraphic(icon);

        btn.setOnAction(e -> NavigationService.getInstance().navigateTo(route));
        return btn;
    }

    private void highlightActiveNavButton(ViewType activeView) {
        for (Map.Entry<ViewType, Button> entry : navButtonsMap.entrySet()) {
            Button btn = entry.getValue();
            boolean isActive = entry.getKey() == activeView;

            if (isActive) {
                if (!btn.getStyleClass().contains("nav-button-active")) {
                    btn.getStyleClass().add("nav-button-active");
                }
                if (btn.getGraphic() instanceof FontIcon fi) {
                    fi.setStyle("-fx-icon-color: #FFFFFF;");
                }
            } else {
                btn.getStyleClass().remove("nav-button-active");
                if (btn.getGraphic() instanceof FontIcon fi) {
                    fi.setStyle("-fx-icon-color: #94A3B8;");
                }
            }
        }
    }
}
