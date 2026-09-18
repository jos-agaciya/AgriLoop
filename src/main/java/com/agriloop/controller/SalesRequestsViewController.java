package com.agriloop.controller;

import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.OrderItem;
import com.agriloop.model.User;
import com.agriloop.model.enums.OrderStatus;
import com.agriloop.model.enums.UserRole;
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
 * Controller for Farmer incoming Purchase Requests and Sales Pipeline.
 */
public class SalesRequestsViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SalesRequestsViewController.class);

    @FXML private Button refreshBtn;
    @FXML private StackPane refreshIconBox;
    @FXML private VBox requestsContainer;
    @FXML private Label countLabel;

    private final OrderService orderService = OrderService.getInstance();
    private final DeliveryService deliveryService = DeliveryService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon refreshIcon = IconHelper.createIcon(Feather.REFRESH_CW, 14);
        refreshIcon.setStyle("-fx-icon-color: #475569;");
        refreshIconBox.getChildren().add(refreshIcon);

        refreshBtn.setOnAction(e -> loadRequests());
        loadRequests();
    }

    private void loadRequests() {
        requestsContainer.getChildren().clear();
        User current = ProfileService.getInstance().getCurrentUser();
        if (current == null || current.getId() == null) return;

        List<Order> requests = List.of();
        try {
            requests = orderService.getSalesRequestsForFarmer(current.getId());
        } catch (Exception e) {
            logger.error("Failed to load farmer purchase requests", e);
        }

        if (countLabel != null) {
            countLabel.setText(requests.size() + " sales requests");
        }

        if (requests.isEmpty()) {
            EmptyStateCard emptyState = new EmptyStateCard(
                Feather.INBOX,
                "No purchase requests received yet",
                "When verified manufacturing buyers place purchase orders against your waste listings, incoming requests will appear here for review.",
                "View My Listings",
                () -> NavigationService.getInstance().navigateTo(ViewType.MY_LISTINGS)
            );
            requestsContainer.getChildren().add(emptyState);
        } else {
            for (Order order : requests) {
                requestsContainer.getChildren().add(createRequestCard(order, current.getId()));
            }
        }
    }

    private VBox createRequestCard(Order order, Long farmerId) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20px;");

        // Top Row: Order Number, Date, Status Badge
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox idCol = new VBox(2);
        HBox.setHgrow(idCol, Priority.ALWAYS);
        Label orderNumLbl = new Label("Request #" + order.getOrderNumber());
        orderNumLbl.getStyleClass().add("heading-3");
        Label dateLbl = new Label("Received: " + (order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate().toString() : "Recent"));
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

        String materialName = "Agricultural Waste";
        String qtyText = "-";
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem item = order.getItems().get(0);
            materialName = item.getListingTitle() != null ? item.getListingTitle() : "Agri-Residue";
            qtyText = String.format("%.2f Tons @ ₹%.2f/Ton", item.getQuantityTons(), item.getUnitPrice());
        }

        VBox buyerCol = createDetailCell("Buyer / Facility", order.getBuyerName() != null ? order.getBuyerName() : "Industrial Buyer");
        VBox materialCol = createDetailCell("Material Lot", materialName);
        VBox qtyCol = createDetailCell("Quantity & Rate", qtyText);
        VBox amountCol = createDetailCell("Total Amount", String.format("₹%.2f", order.getTotalAmount()));
        VBox addressCol = createDetailCell("Delivery Destination", order.getDeliveryAddress());

        detailsGrid.getChildren().addAll(buyerCol, materialCol, qtyCol, amountCol, addressCol);

        // Bottom Row: Action Controls / Logistics info
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        if (order.getStatus() == OrderStatus.PENDING) {
            Button rejectBtn = new Button("Decline Request");
            rejectBtn.getStyleClass().add("btn-secondary");
            rejectBtn.setOnAction(e -> handleReject(order, farmerId));

            Button acceptBtn = new Button("Accept & Confirm Order");
            acceptBtn.getStyleClass().add("btn-primary");
            acceptBtn.setOnAction(e -> handleAccept(order, farmerId));

            actionRow.getChildren().addAll(rejectBtn, acceptBtn);
        } else if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.IN_TRANSIT) {
            Optional<Delivery> deliveryOpt = deliveryService.getDeliveryForOrder(order.getId());
            if (deliveryOpt.isPresent()) {
                Delivery d = deliveryOpt.get();
                Label trackLbl = new Label("Logistics: Tracking #" + d.getTrackingCode() + " • Status: " + d.getStatus().getLabel());
                trackLbl.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold; -fx-font-size: 13px;");
                actionRow.getChildren().add(trackLbl);
            }
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            Label doneLbl = new Label("Order Fulfilled • Payment Released from Escrow");
            doneLbl.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            actionRow.getChildren().add(doneLbl);
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

    private void handleAccept(Order order, Long farmerId) {
        CompactModal.showConfirmation(
            MainLayoutController.getInstance().getRootStackPane(),
            "Accept Purchase Request",
            "Accept Order #" + order.getOrderNumber() + " for ₹" + order.getTotalAmount() + "? This will reserve the material inventory and create a logistics transport dispatch.",
            "Confirm Acceptance",
            () -> {
                try {
                    orderService.acceptPurchaseRequest(farmerId, order.getId());
                    CompactModal.showSuccess(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Order Confirmed",
                        "Order #" + order.getOrderNumber() + " confirmed. Transport dispatch has been scheduled for pickup.",
                        this::loadRequests
                    );
                } catch (Exception e) {
                    logger.error("Failed to accept order", e);
                    CompactModal.showError(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Action Failed",
                        "Could not accept order: " + e.getMessage(),
                        null
                    );
                }
            },
            null
        );
    }

    private void handleReject(Order order, Long farmerId) {
        CompactModal.showConfirmation(
            MainLayoutController.getInstance().getRootStackPane(),
            "Decline Purchase Request",
            "Are you sure you want to decline Order #" + order.getOrderNumber() + "? The buyer will be notified.",
            "Decline Request",
            () -> {
                try {
                    orderService.rejectPurchaseRequest(farmerId, order.getId(), "Inventory reserved for another contract");
                    CompactModal.showSuccess(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Request Declined",
                        "Purchase request #" + order.getOrderNumber() + " has been declined.",
                        this::loadRequests
                    );
                } catch (Exception e) {
                    logger.error("Failed to reject order", e);
                    CompactModal.showError(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Action Failed",
                        "Could not decline request: " + e.getMessage(),
                        null
                    );
                }
            },
            null
        );
    }
}
