package com.agriloop.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/**
 * Subtle and fast UI animation utilities for a polished desktop experience.
 */
public class AnimationHelper {

    public static void fadeIn(Node node, Duration duration, Runnable onFinished) {
        if (node == null) return;
        node.setOpacity(0);
        node.setVisible(true);

        FadeTransition ft = new FadeTransition(duration != null ? duration : Duration.millis(220), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_OUT);
        if (onFinished != null) {
            ft.setOnFinished(e -> onFinished.run());
        }
        ft.play();
    }

    public static void fadeSlideIn(Node node, double translateY, Duration duration) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(translateY);

        FadeTransition ft = new FadeTransition(duration != null ? duration : Duration.millis(250), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(duration != null ? duration : Duration.millis(250), node);
        tt.setFromY(translateY);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    public static void setupCardHoverAnimation(Node card) {
        if (card == null) return;

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), card);
        scaleUp.setToX(1.015);
        scaleUp.setToY(1.015);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), card);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_OUT);

        card.setOnMouseEntered(e -> {
            scaleDown.stop();
            scaleUp.playFromStart();
        });

        card.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.playFromStart();
        });
    }

    public static void pulse(Node node) {
        if (node == null) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(0.96);
        st.setToY(0.96);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }
}
