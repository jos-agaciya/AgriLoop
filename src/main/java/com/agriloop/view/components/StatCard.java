package com.agriloop.view.components;

import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable Metric / KPI Stat Card component.
 */
public class StatCard extends HBox {

    public enum StatTheme {
        EMERALD("stat-icon-emerald", "#059669"),
        BLUE("stat-icon-blue", "#2563EB"),
        AMBER("stat-icon-amber", "#D97706"),
        PURPLE("stat-icon-purple", "#7C3AED");

        public final String cssClass;
        public final String iconHex;

        StatTheme(String cssClass, String iconHex) {
            this.cssClass = cssClass;
            this.iconHex = iconHex;
        }
    }

    private final Label valueLabel;
    private final Label titleLabel;
    private final Label subtextLabel;

    public StatCard(Feather icon, String title, String value, String subtext, StatTheme theme) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        getStyleClass().add("stat-card");
        HBox.setHgrow(this, Priority.ALWAYS);

        // Icon Box
        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().addAll("stat-icon-wrapper", theme.cssClass);
        FontIcon fontIcon = IconHelper.createIcon(icon, 24);
        fontIcon.setStyle("-fx-icon-color: " + theme.iconHex + ";");
        iconBox.getChildren().add(fontIcon);

        // Text Box
        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        subtextLabel = new Label(subtext);
        subtextLabel.getStyleClass().add("stat-subtext");

        textBox.getChildren().addAll(titleLabel, valueLabel, subtextLabel);
        getChildren().addAll(iconBox, textBox);

        AnimationHelper.setupCardHoverAnimation(this);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setSubtext(String subtext) {
        subtextLabel.setText(subtext);
    }
}
