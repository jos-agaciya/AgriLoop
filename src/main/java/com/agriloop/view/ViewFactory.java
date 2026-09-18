package com.agriloop.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Factory for instantiating role-aware application FXML views.
 * Loads fresh view hierarchies ensuring live database queries on every navigation.
 */
public class ViewFactory {
    private static final Logger logger = LoggerFactory.getLogger(ViewFactory.class);
    private static ViewFactory instance;

    private ViewFactory() {}

    public static synchronized ViewFactory getInstance() {
        if (instance == null) {
            instance = new ViewFactory();
        }
        return instance;
    }

    public Node getView(ViewType viewType) {
        if (viewType == null) {
            throw new IllegalArgumentException("ViewType cannot be null");
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewType.getFxmlPath()));
            Parent node = loader.load();
            logger.info("Loaded fresh view for: {}", viewType.name());
            return node;
        } catch (IOException e) {
            logger.error("Failed to load FXML view for: " + viewType.name() + " at " + viewType.getFxmlPath(), e);
            throw new RuntimeException("Could not load view: " + viewType.name(), e);
        }
    }

    public void clearCache() {
        // No-op for fresh view loading strategy
    }
}
