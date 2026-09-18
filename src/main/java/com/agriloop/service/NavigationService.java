package com.agriloop.service;

import com.agriloop.model.enums.UserRole;
import com.agriloop.util.AnimationHelper;
import com.agriloop.view.ViewFactory;
import com.agriloop.view.ViewType;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service managing view navigation, role switching, and dynamic transitions.
 */
public class NavigationService {
    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);
    private static NavigationService instance;

    private StackPane contentArea;
    private ViewType currentView = ViewType.DASHBOARD;
    private String pendingSearchQuery;
    private final List<Consumer<ViewType>> navigationListeners = new ArrayList<>();
    private final List<Consumer<UserRole>> roleChangeListeners = new ArrayList<>();

    private NavigationService() {}

    public static synchronized NavigationService getInstance() {
        if (instance == null) {
            instance = new NavigationService();
        }
        return instance;
    }

    public void setContentArea(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public ViewType getCurrentView() {
        return currentView;
    }

    public String getPendingSearchQuery() {
        String query = this.pendingSearchQuery;
        this.pendingSearchQuery = null; // consume once
        return query;
    }

    public void navigateToWithSearch(ViewType viewType, String searchQuery) {
        this.pendingSearchQuery = searchQuery;
        navigateTo(viewType);
    }

    public void navigateTo(ViewType viewType) {
        if (viewType == null) return;
        this.currentView = viewType;

        if (contentArea != null) {
            try {
                Node newView = ViewFactory.getInstance().getView(viewType);
                contentArea.getChildren().clear();
                contentArea.getChildren().add(newView);
                AnimationHelper.fadeSlideIn(newView, 8, Duration.millis(180));
            } catch (Exception e) {
                logger.error("Failed to load and navigate to view: {}", viewType, e);
            }
        }

        notifyNavListeners(viewType);
    }

    public void switchRole(UserRole role) {
        if (role == null) return;
        logger.info("Switching application role view context to: {}", role.name());
        for (Consumer<UserRole> listener : roleChangeListeners) {
            try {
                listener.accept(role);
            } catch (Exception e) {
                logger.warn("Error notifying role change listener", e);
            }
        }
        // Navigate back to Dashboard upon switching role
        navigateTo(ViewType.DASHBOARD);
    }

    public void addNavigationListener(Consumer<ViewType> listener) {
        if (listener != null) {
            navigationListeners.add(listener);
            listener.accept(currentView);
        }
    }

    public void addRoleChangeListener(Consumer<UserRole> listener) {
        if (listener != null) {
            roleChangeListeners.add(listener);
        }
    }

    private void notifyNavListeners(ViewType viewType) {
        for (Consumer<ViewType> listener : navigationListeners) {
            try {
                listener.accept(viewType);
            } catch (Exception e) {
                logger.warn("Error notifying nav listener", e);
            }
        }
    }
}
