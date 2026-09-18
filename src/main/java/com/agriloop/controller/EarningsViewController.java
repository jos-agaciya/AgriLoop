package com.agriloop.controller;

import com.agriloop.database.DatabaseManager;
import com.agriloop.model.Delivery;
import com.agriloop.model.Transaction;
import com.agriloop.model.User;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.DeliveryRepository;
import com.agriloop.repository.TransactionRepository;
import com.agriloop.repository.impl.JdbcDeliveryRepository;
import com.agriloop.repository.impl.JdbcTransactionRepository;
import com.agriloop.service.NavigationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.EmptyStateCard;
import com.agriloop.view.components.StatCard;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Financial Settlements & Transporter Payouts View.
 */
public class EarningsViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(EarningsViewController.class);

    @FXML private HBox earningsStatsRow;
    @FXML private VBox transactionsContainer;

    private final DeliveryRepository deliveryRepo = new JdbcDeliveryRepository();
    private final TransactionRepository transactionRepo = new JdbcTransactionRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStats();
        loadTransactions();
    }

    private void setupStats() {
        earningsStatsRow.getChildren().clear();
        User current = ProfileService.getInstance().getCurrentUser();
        Long userId = current != null ? current.getId() : null;

        BigDecimal settledRevenue = BigDecimal.ZERO;
        BigDecimal inTransitPending = BigDecimal.ZERO;
        long completedDeliveries = 0;

        if (userId != null) {
            settledRevenue = querySum("SELECT COALESCE(SUM(delivery_cost), 0) FROM deliveries WHERE transporter_id = ? AND status = 'DELIVERED'", userId);
            inTransitPending = querySum("SELECT COALESCE(SUM(delivery_cost), 0) FROM deliveries WHERE transporter_id = ? AND status IN ('ACCEPTED', 'PICKED_UP', 'IN_TRANSIT')", userId);
            completedDeliveries = queryCount("SELECT COUNT(*) FROM deliveries WHERE transporter_id = ? AND status = 'DELIVERED'", userId);
        }

        StatCard totalEarnings = new StatCard(Feather.DOLLAR_SIGN, "Settled Revenue", String.format("₹%.2f", settledRevenue), "Direct haulage payouts", StatCard.StatTheme.EMERALD);
        StatCard pendingEscrow = new StatCard(Feather.CLOCK, "In-Transit Pending", String.format("₹%.2f", inTransitPending), "Releases upon delivery", StatCard.StatTheme.AMBER);
        StatCard completedPayouts = new StatCard(Feather.CHECK_CIRCLE, "Completed Trips", String.valueOf(completedDeliveries), "Fulfilled shipments", StatCard.StatTheme.BLUE);

        earningsStatsRow.getChildren().addAll(totalEarnings, pendingEscrow, completedPayouts);
    }

    private void loadTransactions() {
        transactionsContainer.getChildren().clear();
        User current = ProfileService.getInstance().getCurrentUser();
        if (current == null || current.getId() == null) {
            transactionsContainer.getChildren().add(new EmptyStateCard(
                Feather.DOLLAR_SIGN,
                "Authentication Required",
                "Please sign in to view your payout history.",
                "Sign In",
                () -> {}
            ));
            return;
        }

        List<Delivery> deliveredList = deliveryRepo.findByTransporterId(current.getId()).stream()
            .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
            .toList();

        if (deliveredList.isEmpty()) {
            EmptyStateCard emptyState = new EmptyStateCard(
                Feather.DOLLAR_SIGN,
                "Your earnings will appear here after completing deliveries",
                "When you accept cargo dispatch jobs and deliver agricultural waste orders to manufacturing plants, settled freight payouts will appear in this ledger.",
                "View Delivery Requests",
                () -> NavigationService.getInstance().navigateTo(ViewType.DELIVERY_REQUESTS)
            );
            transactionsContainer.getChildren().add(emptyState);
        } else {
            for (Delivery d : deliveredList) {
                HBox card = new HBox(16);
                card.setAlignment(Pos.CENTER_LEFT);
                card.getStyleClass().add("card");
                card.setStyle("-fx-padding: 16px 20px;");

                VBox left = new VBox(3);
                HBox.setHgrow(left, Priority.ALWAYS);
                Label title = new Label("Freight Payout • Shipment #" + d.getTrackingCode());
                title.getStyleClass().add("heading-3");
                Label route = new Label(String.format("Route: %s → %s • Distance: %.1f km", 
                    d.getPickupLocation(), d.getDeliveryLocation(), d.getDistanceKm() != null ? d.getDistanceKm().doubleValue() : 45.0));
                route.getStyleClass().add("subheading");
                left.getChildren().addAll(title, route);

                VBox right = new VBox(2);
                right.setAlignment(Pos.CENTER_RIGHT);
                Label amount = new Label(String.format("+ ₹%.2f", d.getDeliveryCost()));
                amount.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #059669;");
                Label status = new Label("SETTLED");
                status.getStyleClass().addAll("badge", "badge-success");
                right.getChildren().addAll(amount, status);

                card.getChildren().addAll(left, right);
                AnimationHelper.setupCardHoverAnimation(card);
                transactionsContainer.getChildren().add(card);
            }
        }
    }

    private BigDecimal querySum(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal val = rs.getBigDecimal(1);
                    return val != null ? val : BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            logger.warn("Query sum failed", e);
        }
        return BigDecimal.ZERO;
    }

    private long queryCount(String sql, Object... params) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.warn("Query count failed", e);
        }
        return 0;
    }
}
