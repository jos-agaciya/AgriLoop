package com.agriloop;

import com.agriloop.config.AppConfig;
import com.agriloop.database.DatabaseManager;
import com.agriloop.model.User;
import com.agriloop.service.NavigationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.FontManager;
import com.agriloop.view.ViewType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * AgriLoop Desktop Application Entrypoint.
 * Starts with the Login/Register screen and manages session transitions.
 */
public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static Stage primaryStage;

    @Override
    public void init() {
        logger.info("Initializing AgriLoop Application...");

        // Load and register Google Sans font family globally
        FontManager.loadFonts();

        // Initialize Database Connection Pool asynchronously
        new Thread(() -> {
            try {
                DatabaseManager.getInstance().initializePool();
            } catch (Exception e) {
                logger.warn("Initial database check: {}", e.getMessage());
            }
        }, "DB-Init-Thread").start();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle(AppConfig.WINDOW_TITLE);
        primaryStage.setMinWidth(AppConfig.MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(AppConfig.MIN_WINDOW_HEIGHT);

        try {
            primaryStage.getIcons().add(new javafx.scene.image.Image(App.class.getResourceAsStream("/images/agriloop-app-icon.png")));
        } catch (Exception e) {
            logger.warn("Could not load stage window icon", e);
        }

        // Graceful shutdown hook
        primaryStage.setOnCloseRequest(event -> {
            logger.info("Application shutting down.");
            DatabaseManager.getInstance().closePool();
            Platform.exit();
            System.exit(0);
        });

        // Launch directly into Authentication Screen
        showAuthScreen();
    }

    /**
     * Navigates to the Authentication (Sign In / Register) screen.
     */
    public static void showAuthScreen() {
        try {
            ProfileService.getInstance().setCurrentUser(null);
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/AuthView.fxml"));
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, AppConfig.DEFAULT_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_HEIGHT);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            AnimationHelper.fadeIn(root, null, null);
            primaryStage.show();
            logger.info("Authentication view loaded.");
        } catch (IOException e) {
            logger.error("Failed to load AuthView.fxml", e);
        }
    }

    /**
     * Navigates to the Main Application Shell for an authenticated user.
     */
    public static void showMainApp(User authenticatedUser) {
        try {
            ProfileService.getInstance().setCurrentUser(authenticatedUser);

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/MainLayout.fxml"));
            Parent root = loader.load();

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root, AppConfig.DEFAULT_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_HEIGHT);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            // Route dynamically based on the authenticated user's role
            NavigationService.getInstance().switchRole(authenticatedUser.getRole());
            NavigationService.getInstance().navigateTo(ViewType.DASHBOARD);

            AnimationHelper.fadeIn(root, null, null);
            logger.info("Main application shell loaded for user: {}", authenticatedUser.getEmail());
        } catch (IOException e) {
            logger.error("Failed to load MainLayout.fxml", e);
        }
    }

    @Override
    public void stop() {
        DatabaseManager.getInstance().closePool();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
