package com.agriloop;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.IkonHandler;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.feather.FeatherIkonHandler;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.scene.control.Button;
import java.io.InputStream;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class IconDiagnosisTest {

    @BeforeAll
    public static void init() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testAllApplicationIconsRender() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger renderedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Platform.runLater(() -> {
            try {
                VBox container = new VBox(10);
                Scene scene = new Scene(container, 800, 600);
                scene.getStylesheets().addAll(
                    getClass().getResource("/css/variables.css").toExternalForm(),
                    getClass().getResource("/css/main.css").toExternalForm(),
                    getClass().getResource("/css/sidebar.css").toExternalForm(),
                    getClass().getResource("/css/components.css").toExternalForm(),
                    getClass().getResource("/css/auth.css").toExternalForm()
                );

                // 1. Audit ViewType routes
                for (com.agriloop.view.ViewType vt : com.agriloop.view.ViewType.values()) {
                    FontIcon fi = com.agriloop.util.IconHelper.createIcon(vt.getIcon(), 18);
                    Button b = new Button(vt.getTitle(), fi);
                    b.getStyleClass().add("nav-button");
                    container.getChildren().add(b);
                    renderedCount.incrementAndGet();
                }

                // 2. Audit StatCard icons
                Feather[] statIcons = {
                    Feather.PACKAGE, Feather.LAYERS, Feather.FILE_TEXT, Feather.CHECK_CIRCLE,
                    Feather.DOLLAR_SIGN, Feather.CLIPBOARD, Feather.TRUCK, Feather.INBOX,
                    Feather.MAP_PIN, Feather.ACTIVITY, Feather.SHIELD, Feather.ALERT_TRIANGLE,
                    Feather.CPU, Feather.CLOCK
                };
                for (Feather fi : statIcons) {
                    com.agriloop.view.components.StatCard sc = new com.agriloop.view.components.StatCard(
                        fi, "Test Metric", "100.00", "Subtitle", com.agriloop.view.components.StatCard.StatTheme.EMERALD
                    );
                    container.getChildren().add(sc);
                    renderedCount.incrementAndGet();
                }

                // 3. Audit Header / Action / Notification / Modal Icons
                Feather[] miscIcons = {
                    Feather.SEARCH, Feather.BELL, Feather.USER, Feather.LOG_OUT,
                    Feather.PLUS_CIRCLE, Feather.PLUS, Feather.REFRESH_CW, Feather.CAMERA,
                    Feather.X, Feather.HELP_CIRCLE, Feather.INFO, Feather.TRENDING_UP
                };
                for (Feather fi : miscIcons) {
                    FontIcon icon = com.agriloop.util.IconHelper.createIcon(fi, 16);
                    container.getChildren().add(icon);
                    renderedCount.incrementAndGet();
                }

                container.applyCss();
                container.layout();

                System.out.println("Audited and rendered " + renderedCount.get() + " icons in full JavaFX CSS scene graph successfully.");
                org.junit.jupiter.api.Assertions.assertTrue(renderedCount.get() >= 30, "Should have audited at least 30 icons");

            } catch (Exception e) {
                e.printStackTrace();
                org.junit.jupiter.api.Assertions.fail("Icon rendering failed with exception: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void diagnoseIkonli() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                System.out.println("=== DIAGNOSING IKONLI ===");
                
                // 1. Check ServiceLoader
                ServiceLoader<IkonHandler> handlers = ServiceLoader.load(IkonHandler.class);
                System.out.println("Discovered IkonHandlers:");
                boolean foundFeather = false;
                for (IkonHandler handler : handlers) {
                    System.out.println(" - " + handler.getClass().getName() + " handles: " + handler.supports("feather-home"));
                    if (handler instanceof FeatherIkonHandler) foundFeather = true;
                }
                System.out.println("Found FeatherIkonHandler via ServiceLoader: " + foundFeather);

                // 2. Check Feather handler font
                FeatherIkonHandler fHandler = new FeatherIkonHandler();
                java.net.URL fontUrl = fHandler.getFontResource();
                System.out.println("Feather handler font URL: " + fontUrl);
                if (fontUrl != null) {
                    try (InputStream in = fontUrl.openStream()) {
                        Font loaded = Font.loadFont(in, 16);
                        System.out.println("Loaded font from resource: " + (loaded != null ? loaded.getName() + " / " + loaded.getFamily() : "NULL"));
                    }
                }

                // 3. Create FontIcon in Button with Sidebar styling
                Button btn = new Button("Dashboard");
                btn.getStyleClass().add("nav-button");
                FontIcon icon = new FontIcon(Feather.HOME);
                icon.setIconSize(18);
                btn.setGraphic(icon);

                VBox root = new VBox(btn);
                Scene scene = new Scene(root, 300, 200);
                scene.getStylesheets().addAll(
                    getClass().getResource("/css/variables.css").toExternalForm(),
                    getClass().getResource("/css/main.css").toExternalForm(),
                    getClass().getResource("/css/sidebar.css").toExternalForm(),
                    getClass().getResource("/css/components.css").toExternalForm()
                );
                
                // Trigger full CSS styling pass
                root.applyCss();
                root.layout();

                System.out.println("--- CHECKING FONTICON AFTER FULL CSS PASS ---");
                System.out.println("Root font: " + root.getStyle());
                System.out.println("Button font: " + btn.getFont());
                System.out.println("FontIcon font: " + icon.getFont());
                System.out.println("FontIcon style classes: " + icon.getStyleClass());
                System.out.println("FontIcon inline style: " + icon.getStyle());
                System.out.println("FontIcon font family: " + icon.getFont().getFamily());
                System.out.println("FontIcon font name: " + icon.getFont().getName());

                // Now check if FontIcon has its font family replaced by Google Sans!
                boolean isOverriddenByGoogleSans = "Google Sans".equalsIgnoreCase(icon.getFont().getFamily());
                System.out.println("IS FONTICON OVERRIDDEN BY GOOGLE SANS? " + isOverriddenByGoogleSans);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
    }
}
