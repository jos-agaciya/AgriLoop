package com.agriloop.controller;

import com.agriloop.App;
import com.agriloop.database.DatabaseManager;
import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import com.agriloop.service.ProfileService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import com.agriloop.util.SecurityUtil;
import com.agriloop.util.ValidationUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the customer-facing Authentication screen (Sign In & Account Registration).
 */
public class AuthViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AuthViewController.class);

    @FXML private StackPane heroBrandIconBox;
    @FXML private StackPane heroFeatureIcon1;
    @FXML private StackPane heroFeatureIcon2;
    @FXML private StackPane heroFeatureIcon3;

    @FXML private Button tabSignInBtn;
    @FXML private Button tabRegisterBtn;

    // Sign In Form Container & Fields
    @FXML private VBox signInFormContainer;
    @FXML private TextField loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button signInSubmitBtn;
    @FXML private Button switchToRegisterLink;

    // Register Form Container & Fields
    @FXML private VBox registerFormContainer;
    @FXML private TextField regFullNameField;
    @FXML private TextField regEmailField;
    @FXML private TextField regPhoneField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;
    @FXML private TextField regLocationField;
    @FXML private ComboBox<UserRole> regRoleCombo;
    @FXML private Button registerSubmitBtn;
    @FXML private Button switchToSignInLink;

    // Feedback Banners
    @FXML private HBox authErrorBanner;
    @FXML private Label authErrorText;
    @FXML private HBox authSuccessBanner;
    @FXML private Label authSuccessText;

    private final UserRepository userRepository = new JdbcUserRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHeroIcons();
        setupRoleDropdown();

        hideFeedback();
        showSignInTab();

        tabSignInBtn.setOnAction(e -> {
            hideFeedback();
            showSignInTab();
        });
        tabRegisterBtn.setOnAction(e -> {
            hideFeedback();
            showRegisterTab();
        });
        switchToRegisterLink.setOnAction(e -> {
            hideFeedback();
            showRegisterTab();
        });
        switchToSignInLink.setOnAction(e -> {
            hideFeedback();
            showSignInTab();
        });

        signInSubmitBtn.setOnAction(e -> handleSignIn());
        registerSubmitBtn.setOnAction(e -> handleRegister());

        loginEmailField.setOnAction(e -> handleSignIn());
        loginPasswordField.setOnAction(e -> handleSignIn());
    }

    private void setupHeroIcons() {
        try {
            javafx.scene.image.Image logoImg = new javafx.scene.image.Image(getClass().getResourceAsStream("/images/agriloop-app-icon.png"), 44, 44, true, true);
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logoImg);
            logoView.setFitWidth(44);
            logoView.setFitHeight(44);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(44, 44);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            logoView.setClip(clip);

            heroBrandIconBox.getChildren().clear();
            heroBrandIconBox.getChildren().add(logoView);
            heroBrandIconBox.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-alignment: CENTER;");
        } catch (Exception e) {
            FontIcon brandIcon = IconHelper.createIcon(Feather.PACKAGE, 24);
            brandIcon.setStyle("-fx-icon-color: #FFFFFF;");
            heroBrandIconBox.getChildren().add(brandIcon);
        }

        FontIcon feat1 = IconHelper.createIcon(Feather.TRENDING_UP, 18);
        feat1.setStyle("-fx-icon-color: #10B981;");
        heroFeatureIcon1.getChildren().add(feat1);

        FontIcon feat2 = IconHelper.createIcon(Feather.SHIELD, 18);
        feat2.setStyle("-fx-icon-color: #10B981;");
        heroFeatureIcon2.getChildren().add(feat2);

        FontIcon feat3 = IconHelper.createIcon(Feather.GLOBE, 18);
        feat3.setStyle("-fx-icon-color: #10B981;");
        heroFeatureIcon3.getChildren().add(feat3);
    }

    private void setupRoleDropdown() {
        regRoleCombo.getItems().clear();
        regRoleCombo.getItems().addAll(UserRole.FARMER, UserRole.MANUFACTURER, UserRole.TRANSPORTER);
        regRoleCombo.setValue(UserRole.FARMER);

        regRoleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UserRole item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        regRoleCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(UserRole item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
    }

    private void showSignInTab() {
        tabSignInBtn.getStyleClass().remove("auth-tab-btn-active");
        tabSignInBtn.getStyleClass().add("auth-tab-btn-active");
        tabRegisterBtn.getStyleClass().remove("auth-tab-btn-active");

        signInFormContainer.setVisible(true);
        signInFormContainer.setManaged(true);
        registerFormContainer.setVisible(false);
        registerFormContainer.setManaged(false);

        AnimationHelper.fadeIn(signInFormContainer, null, null);
    }

    private void showRegisterTab() {
        tabRegisterBtn.getStyleClass().remove("auth-tab-btn-active");
        tabRegisterBtn.getStyleClass().add("auth-tab-btn-active");
        tabSignInBtn.getStyleClass().remove("auth-tab-btn-active");

        registerFormContainer.setVisible(true);
        registerFormContainer.setManaged(true);
        signInFormContainer.setVisible(false);
        signInFormContainer.setManaged(false);

        AnimationHelper.fadeIn(registerFormContainer, null, null);
    }

    private void handleSignIn() {
        hideFeedback();
        String email = loginEmailField.getText() != null ? loginEmailField.getText().trim() : "";
        String password = loginPasswordField.getText();

        if (email.isBlank() || password == null || password.isBlank()) {
            showError("Please enter your email and password.");
            return;
        }

        if (!DatabaseManager.getInstance().isConnected()) {
            showError("Cannot connect to server. Please check your database connection.");
            return;
        }

        try {
            User user = com.agriloop.service.AuthService.getInstance().login(email, password);
            logger.info("User authenticated successfully: {} (Role: {})", user.getEmail(), user.getRole());
            ProfileService.getInstance().setCurrentUser(user);
            App.showMainApp(user);
        } catch (com.agriloop.exception.ValidationException ve) {
            showError(ve.getMessage());
        } catch (Exception e) {
            logger.error("Authentication error", e);
            showError("An error occurred during sign in. Please try again.");
        }
    }

    private void handleRegister() {
        hideFeedback();
        String fullName = regFullNameField.getText() != null ? regFullNameField.getText().trim() : "";
        String email = regEmailField.getText() != null ? regEmailField.getText().trim() : "";
        String phone = regPhoneField.getText() != null ? regPhoneField.getText().trim() : "";
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();
        String location = regLocationField.getText() != null ? regLocationField.getText().trim() : "";
        UserRole role = regRoleCombo.getValue();

        if (fullName.isBlank() || email.isBlank() || password == null || password.isBlank()) {
            showError("Please fill in all required fields.");
            return;
        }

        if (!DatabaseManager.getInstance().isConnected()) {
            showError("Cannot connect to server. Please check your database connection.");
            return;
        }

        try {
            User newUser = com.agriloop.service.AuthService.getInstance().register(
                fullName, email, phone, password, confirmPassword, location, role
            );

            logger.info("Registered new user successfully: {} with role: {}", newUser.getEmail(), newUser.getRole());

            // Clear registration fields
            regFullNameField.clear();
            regEmailField.clear();
            regPhoneField.clear();
            regPasswordField.clear();
            regConfirmPasswordField.clear();
            regLocationField.clear();

            // Switch to Sign In tab and prefill email
            loginEmailField.setText(email);
            loginPasswordField.clear();
            showSignInTab();

            // Display product-oriented success confirmation
            showSuccess("Account created successfully. Please sign in to continue.");
        } catch (com.agriloop.exception.ValidationException ve) {
            showError(ve.getMessage());
        } catch (Exception e) {
            logger.error("Registration error", e);
            showError("Failed to create account: " + e.getMessage());
        }
    }

    private void showError(String message) {
        hideFeedback();
        authErrorText.setText(message);
        authErrorBanner.setVisible(true);
        authErrorBanner.setManaged(true);
        AnimationHelper.pulse(authErrorBanner);
    }

    private void showSuccess(String message) {
        hideFeedback();
        authSuccessText.setText(message);
        authSuccessBanner.setVisible(true);
        authSuccessBanner.setManaged(true);
        AnimationHelper.pulse(authSuccessBanner);
    }

    private void hideFeedback() {
        authErrorBanner.setVisible(false);
        authErrorBanner.setManaged(false);
        authErrorText.setText("");

        authSuccessBanner.setVisible(false);
        authSuccessBanner.setManaged(false);
        authSuccessText.setText("");
    }
}

