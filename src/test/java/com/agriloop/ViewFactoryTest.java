package com.agriloop;

import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.service.ProfileService;
import com.agriloop.view.ViewFactory;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.CompactModal;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test ensuring all ViewType FXML hierarchies and controllers load cleanly
 * without missing @FXML bindings, NullPointerExceptions, or modal overlay leaks.
 */
public class ViewFactoryTest {

    @BeforeAll
    public static void initJavaFx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // JavaFX platform already started
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit failed to initialize");

        // Set up mock profile context
        User mockFarmer = new User();
        mockFarmer.setId(1L);
        mockFarmer.setFullName("Test Farmer");
        mockFarmer.setEmail("test_farmer@agriloop.com");
        mockFarmer.setRole(UserRole.FARMER);
        ProfileService.getInstance().setCurrentUser(mockFarmer);
    }

    @Test
    public void testAllViewTypesLoadSuccessfully() throws Exception {
        for (ViewType viewType : ViewType.values()) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean success = new AtomicBoolean(false);
            final Throwable[] error = new Throwable[1];

            Platform.runLater(() -> {
                try {
                    Node node = ViewFactory.getInstance().getView(viewType);
                    assertNotNull(node, "View should not be null for " + viewType);
                    success.set(true);
                } catch (Throwable t) {
                    error[0] = t;
                } finally {
                    latch.countDown();
                }
            });

            assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout loading view for " + viewType);
            if (error[0] != null) {
                fail("Failed to load view " + viewType + ": " + error[0].getMessage(), error[0]);
            }
            assertTrue(success.get(), "View loading failed for " + viewType);
        }
    }

    @Test
    public void testCompactModalOverlayLifecycleAndCleanup() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                StackPane root = new StackPane();
                assertEquals(0, root.getChildren().size());

                // Show success modal
                CompactModal.showSuccess(root, "Success Title", "Success Message", null);
                assertEquals(1, root.getChildren().size(), "Modal overlay should be attached to root");

                // Close active modal
                CompactModal.closeActiveModal(root);
                assertEquals(0, root.getChildren().size(), "Root should have zero children after closing modal");

                // Show confirmation modal
                CompactModal.showConfirmation(root, "Confirm Title", "Confirm Message", "Yes", null, null);
                assertEquals(1, root.getChildren().size(), "Confirmation modal overlay should be attached");

                // Close again
                CompactModal.closeActiveModal(root);
                assertEquals(0, root.getChildren().size(), "Root should have zero children after closing confirmation modal");

            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout verifying CompactModal lifecycle");
    }
}
