package com.agriloop.controller;

import com.agriloop.service.NavigationService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.view.ViewType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Root Controller for the main application shell window.
 */
public class MainLayoutController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MainLayoutController.class);
    private static MainLayoutController instance;

    @FXML private StackPane rootStackPane;
    @FXML private StackPane mainContentHost;
    @FXML private StackPane modalHostPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        modalHostPane.setVisible(false);
        modalHostPane.setManaged(false);
        modalHostPane.setMouseTransparent(true);

        // Register mainContentHost with NavigationService
        NavigationService.getInstance().setContentArea(mainContentHost);

        // Navigate to initial Dashboard view
        NavigationService.getInstance().navigateTo(ViewType.DASHBOARD);
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    public static void openNotificationsModal() {
        if (instance != null) {
            instance.showModal("/fxml/NotificationsModal.fxml");
        }
    }

    public static void closeModal() {
        if (instance != null) {
            instance.modalHostPane.getChildren().clear();
            instance.modalHostPane.setVisible(false);
            instance.modalHostPane.setManaged(false);
            instance.modalHostPane.setMouseTransparent(true);
        }
    }

    public void showModal(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent modalContent = loader.load();
            modalHostPane.getChildren().clear();
            modalHostPane.getChildren().add(modalContent);
            modalHostPane.setVisible(true);
            modalHostPane.setManaged(true);
            modalHostPane.setMouseTransparent(false);
            AnimationHelper.fadeIn(modalHostPane, null, null);
        } catch (IOException e) {
            logger.error("Failed to load modal dialog from: " + fxmlPath, e);
        }
    }

    public StackPane getRootStackPane() {
        return rootStackPane;
    }
}
