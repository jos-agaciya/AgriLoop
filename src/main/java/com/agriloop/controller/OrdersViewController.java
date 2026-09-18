package com.agriloop.controller;

import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.OrderItem;
import com.agriloop.model.User;
import com.agriloop.model.enums.OrderStatus;
import com.agriloop.service.DeliveryService;
import com.agriloop.service.NavigationService;
import com.agriloop.service.OrderService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.CompactModal;
import com.agriloop.view.components.EmptyStateCard;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Manufacturer's My Orders view.
 */
public class OrdersViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(OrdersViewController.class);

    @FXML private VBox ordersContainer;
    @FXML private Button refreshBtn;
    @FXML private StackPane refreshIconBox;

    private final OrderService orderService = OrderService.getInstance();
    private final DeliveryService deliveryService = DeliveryService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon refreshIcon = IconHelper.createIcon(Feather.REFRESH_CW, 14);
        refreshIcon.setStyle("-fx-icon-color: #475569;");
        refreshIconBox.getChildren().add(refreshIcon);

        refreshBtn.setOnAction(e -> loadOrders());
        loadOrders();
    }

    private void loadOrders() {
        ordersContainer.getChildren().clear();

        User current = ProfileService.getInstance().getCurrentUser();
        if (current == null || current.getId() == null) {
            ordersContainer.getChildren().add(new EmptyStateCard(
                Feather.SHOPPING_BAG,
                "Authentication Required",
                "Please sign in as a Manufacturer to view your orders.",
                "Sign In",
                () -> {}
            ));
            return;
        }

        List<Order> orders = List.of();
        try {
            orders = orderService.getOrdersForBuyer(current.getId());
        } catch (Exception e) {
            logger.error("Failed to fetch buyer orders", e);
        }

        if (orders.isEmpty()) {
            EmptyStateCard emptyState = new EmptyStateCard(
                Feather.SHOPPING_BAG,
                "No purchase orders placed yet",
                "Browse agricultural waste listings on the marketplace catalog and submit purchase requests to start your procurement pipeline.",
                "Explore Marketplace",
                () -> NavigationService.getInstance().navigateTo(ViewType.MARKETPLACE)
            );
            ordersContainer.getChildren().add(emptyState);
        } else {
            for (Order order : orders) {
                ordersContainer.getChildren().add(createOrderCard(order, current.getId()));
            }
        }
    }

    private VBox createOrderCard(Order order, Long buyerId) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20px;");

        // Top Row: Order Number, Date, Status Badge
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox idCol = new VBox(2);
        HBox.setHgrow(idCol, Priority.ALWAYS);
        Label orderNumLbl = new Label("Order #" + order.getOrderNumber());
        orderNumLbl.getStyleClass().add("heading-3");
        Label dateLbl = new Label("Ordered: " + (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate().toString() : "Recent"));
        dateLbl.getStyleClass().add("subheading");
        idCol.getChildren().addAll(orderNumLbl, dateLbl);

        Label statusBadge = new Label(order.getStatus().getLabel());
        String badgeClass = switch (order.getStatus()) {
            case PENDING -> "badge-warning";
            case CONFIRMED, PROCESSING, IN_TRANSIT -> "badge-info";
            case DELIVERED, COMPLETED -> "badge-success";
            case CANCELLED -> "badge-danger";
        };
        statusBadge.getStyleClass().addAll("badge", badgeClass);
        topRow.getChildren().addAll(idCol, statusBadge);

        // Middle Row: Details Grid
        HBox detailsGrid = new HBox(24);
        detailsGrid.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 12px 16px; -fx-background-radius: 8px;");

        String materialName = "Agricultural Residue";
        String qtyText = "-";
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem item = order.getItems().get(0);
            materialName = item.getListingTitle() != null ? item.getListingTitle() : "Agri-Residue";
            qtyText = String.format("%.2f Tons @ ₹%.2f/Ton", item.getQuantityTons(), item.getUnitPrice());
        }

        VBox sellerCol = createDetailCell("Seller / Farm", order.getSellerName() != null ? order.getSellerName() : "Registered Farmer");
        VBox materialCol = createDetailCell("Material Ordered", materialName);
        VBox qtyCol = createDetailCell("Quantity & Rate", qtyText);
        VBox amountCol = createDetailCell("Total Value", String.format("₹%.2f", order.getTotalAmount()));
        VBox addressCol = createDetailCell("Destination Address", order.getDeliveryAddress());

        detailsGrid.getChildren().addAll(sellerCol, materialCol, qtyCol, amountCol, addressCol);

        // Bottom Row: Tracking & Actions
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        if (order.getStatus() == OrderStatus.PENDING) {
            Button cancelBtn = new Button("Cancel Request");
            cancelBtn.getStyleClass().add("btn-secondary");
            cancelBtn.setStyle("-fx-text-fill: #DC2626; -fx-border-color: #FECACA;");
            cancelBtn.setOnAction(e -> handleCancel(order, buyerId));
            actionRow.getChildren().add(cancelBtn);
        } else {
            Optional<Delivery> deliveryOpt = deliveryService.getDeliveryForOrder(order.getId());
            if (deliveryOpt.isPresent()) {
                Delivery d = deliveryOpt.get();
                Label trackInfo = new Label(String.format("Logistics Tracking: %s • Carrier: %s • Status: %s",
                    d.getTrackingCode(),
                    d.getTransporterName() != null ? d.getTransporterName() : "Assignment Pending",
                    d.getStatus().getLabel()
                ));
                trackInfo.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 13px;");

                Button trackBtn = new Button("Track Delivery");
                trackBtn.getStyleClass().add("btn-secondary");
                trackBtn.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.DELIVERIES));

                actionRow.getChildren().addAll(trackInfo, trackBtn);
            }
        }

        card.getChildren().addAll(topRow, detailsGrid, actionRow);
        AnimationHelper.setupCardHoverAnimation(card);
        return card;
    }

    private VBox createDetailCell(String label, String value) {
        VBox cell = new VBox(2);
        HBox.setHgrow(cell, Priority.ALWAYS);
        Label title = new Label(label);
        title.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #64748B; -fx-font-weight: 500;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-size: 13px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
        val.setWrapText(true);
        cell.getChildren().addAll(title, val);
        return cell;
    }

    private void handleCancel(Order order, Long buyerId) {
        CompactModal.showConfirmation(
            MainLayoutController.getInstance().getRootStackPane(),
            "Cancel Purchase Order",
            "Are you sure you want to cancel Order #" + order.getOrderNumber() + "?",
            "Cancel Order",
            () -> {
                try {
                    orderService.cancelOrder(buyerId, order.getId());
                    CompactModal.showSuccess(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Order Cancelled",
                        "Order #" + order.getOrderNumber() + " has been cancelled.",
                        this::loadOrders
                    );
                } catch (Exception e) {
                    logger.error("Failed to cancel order", e);
                    CompactModal.showError(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Action Failed",
                        "Could not cancel order: " + e.getMessage(),
                        null
                    );
                }
            },
            null
        );
    }
}
