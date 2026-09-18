package com.agriloop.controller;

import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.User;
import com.agriloop.model.enums.DeliveryStatus;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.DeliveryRepository;
import com.agriloop.repository.OrderRepository;
import com.agriloop.repository.impl.JdbcDeliveryRepository;
import com.agriloop.repository.impl.JdbcOrderRepository;
import com.agriloop.service.DeliveryService;
import com.agriloop.service.NavigationService;
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

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for Logistics Dispatch, Transporter Job Execution, and Buyer Shipment Tracking.
 */
public class DeliveriesViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DeliveriesViewController.class);

    @FXML private Label headerTitleLabel;
    @FXML private Label headerSubtitleLabel;
    @FXML private Button refreshBtn;
    @FXML private StackPane refreshIconBox;

    @FXML private HBox transporterTabRow;
    @FXML private Button activeDeliveriesTabBtn;
    @FXML private Button availableRequestsTabBtn;

    @FXML private VBox deliveriesContainer;

    private final DeliveryService deliveryService = DeliveryService.getInstance();
    private final DeliveryRepository deliveryRepo = new JdbcDeliveryRepository();
    private final OrderRepository orderRepo = new JdbcOrderRepository();

    private boolean showAvailableRequestsTab = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon refreshIcon = IconHelper.createIcon(Feather.REFRESH_CW, 14);
        refreshIcon.setStyle("-fx-icon-color: #475569;");
        refreshIconBox.getChildren().add(refreshIcon);

        refreshBtn.setOnAction(e -> loadDeliveries());

        activeDeliveriesTabBtn.setOnAction(e -> {
            showAvailableRequestsTab = false;
            updateTabStyles();
            loadDeliveries();
        });

        availableRequestsTabBtn.setOnAction(e -> {
            showAvailableRequestsTab = true;
            updateTabStyles();
            loadDeliveries();
        });

        ViewType currentNav = NavigationService.getInstance().getCurrentView();
        showAvailableRequestsTab = (currentNav == ViewType.DELIVERY_REQUESTS);

        loadDeliveries();
    }

    private void updateTabStyles() {
        if (showAvailableRequestsTab) {
            availableRequestsTabBtn.getStyleClass().remove("btn-secondary");
            if (!availableRequestsTabBtn.getStyleClass().contains("btn-primary")) availableRequestsTabBtn.getStyleClass().add("btn-primary");

            activeDeliveriesTabBtn.getStyleClass().remove("btn-primary");
            if (!activeDeliveriesTabBtn.getStyleClass().contains("btn-secondary")) activeDeliveriesTabBtn.getStyleClass().add("btn-secondary");
        } else {
            activeDeliveriesTabBtn.getStyleClass().remove("btn-secondary");
            if (!activeDeliveriesTabBtn.getStyleClass().contains("btn-primary")) activeDeliveriesTabBtn.getStyleClass().add("btn-primary");

            availableRequestsTabBtn.getStyleClass().remove("btn-primary");
            if (!availableRequestsTabBtn.getStyleClass().contains("btn-secondary")) availableRequestsTabBtn.getStyleClass().add("btn-secondary");
        }
    }

    private void loadDeliveries() {
        deliveriesContainer.getChildren().clear();
        User current = ProfileService.getInstance().getCurrentUser();
        UserRole role = current != null ? current.getRole() : UserRole.TRANSPORTER;

        if (role == UserRole.TRANSPORTER) {
            headerTitleLabel.setText("Logistics Dispatches & Deliveries");
            headerSubtitleLabel.setText("Accept available freight assignments and update active shipment milestones");
            transporterTabRow.setVisible(true);
            transporterTabRow.setManaged(true);
            updateTabStyles();

            if (showAvailableRequestsTab) {
                loadAvailableTransporterRequests(current != null ? current.getId() : null);
            } else {
                loadActiveTransporterDeliveries(current != null ? current.getId() : null);
            }
        } else {
            headerTitleLabel.setText("Cargo Tracking & Deliveries");
            headerSubtitleLabel.setText("Monitor transit milestones and carrier dispatches for your materials");
            transporterTabRow.setVisible(false);
            transporterTabRow.setManaged(false);
            loadCustomerDeliveries(current);
        }
    }

    private void loadAvailableTransporterRequests(Long transporterId) {
        List<Delivery> list = List.of();
        try {
            list = deliveryService.getAvailableRequests();
        } catch (Exception e) {
            logger.error("Failed to load available deliveries", e);
        }

        if (list.isEmpty()) {
            deliveriesContainer.getChildren().add(new EmptyStateCard(
                Feather.INBOX,
                "No pending cargo dispatch requests",
                "New logistics requests from verified agricultural transactions will appear here when sellers confirm orders.",
                "Refresh Requests",
                this::loadDeliveries
            ));
        } else {
            for (Delivery d : list) {
                deliveriesContainer.getChildren().add(createAvailableRequestCard(d, transporterId));
            }
        }
    }

    private void loadActiveTransporterDeliveries(Long transporterId) {
        List<Delivery> list = List.of();
        try {
            if (transporterId != null) {
                list = deliveryService.getActiveDeliveries(transporterId);
            }
        } catch (Exception e) {
            logger.error("Failed to load active transporter deliveries", e);
        }

        if (list.isEmpty()) {
            deliveriesContainer.getChildren().add(new EmptyStateCard(
                Feather.TRUCK,
                "No active transit deliveries assigned",
                "You currently have no cargo in transit. Browse and accept pending delivery requests to begin a route.",
                "Browse Requests",
                () -> {
                    showAvailableRequestsTab = true;
                    updateTabStyles();
                    loadDeliveries();
                }
            ));
        } else {
            for (Delivery d : list) {
                deliveriesContainer.getChildren().add(createActiveDeliveryCard(d, transporterId));
            }
        }
    }

    private void loadCustomerDeliveries(User current) {
        if (current == null) return;
        List<Delivery> list = new ArrayList<>();
        try {
            List<Order> orders = orderRepo.findByBuyerId(current.getId());
            for (Order o : orders) {
                Optional<Delivery> dOpt = deliveryRepo.findByOrderId(o.getId());
                dOpt.ifPresent(list::add);
            }
        } catch (Exception e) {
            logger.error("Failed to load customer deliveries", e);
        }

        if (list.isEmpty()) {
            deliveriesContainer.getChildren().add(new EmptyStateCard(
                Feather.TRUCK,
                "No shipments currently dispatched",
                "When your orders are confirmed by the seller, transport dispatches and tracking numbers will appear here.",
                "Browse Materials",
                () -> NavigationService.getInstance().navigateTo(ViewType.MARKETPLACE)
            ));
        } else {
            for (Delivery d : list) {
                deliveriesContainer.getChildren().add(createTrackingOnlyCard(d));
            }
        }
    }

    private VBox createAvailableRequestCard(Delivery d, Long transporterId) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20px;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox idCol = new VBox(2);
        HBox.setHgrow(idCol, Priority.ALWAYS);
        Label title = new Label("Dispatch Request #" + d.getTrackingCode());
        title.getStyleClass().add("heading-3");
        Label sub = new Label("Order #" + (d.getOrderNumber() != null ? d.getOrderNumber() : d.getOrderId()));
        sub.getStyleClass().add("subheading");
        idCol.getChildren().addAll(title, sub);

        Label badge = new Label("READY FOR PICKUP");
        badge.getStyleClass().addAll("badge", "badge-warning");
        topRow.getChildren().addAll(idCol, badge);

        // Details Grid
        HBox grid = new HBox(20);
        grid.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 12px 16px; -fx-background-radius: 8px;");
        
        String distStr = formatDistanceDisplay(d.getDistanceKm());
        String costStr = d.getDeliveryCost() != null ? String.format("₹%.2f", d.getDeliveryCost()) : "₹0.00";

        grid.getChildren().addAll(
            createDetailCell("Pickup Origin", d.getPickupLocation()),
            createDetailCell("Drop Destination", d.getDeliveryLocation()),
            createDetailCell("Route Distance", distStr),
            createDetailCell("Payout Rate", costStr)
        );

        // Actions
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button acceptBtn = new Button("Accept Delivery Assignment");
        acceptBtn.getStyleClass().add("btn-primary");
        acceptBtn.setOnAction(e -> handleAcceptDelivery(d, transporterId));
        actionRow.getChildren().add(acceptBtn);

        card.getChildren().addAll(topRow, grid, actionRow);
        AnimationHelper.setupCardHoverAnimation(card);
        return card;
    }

    private VBox createActiveDeliveryCard(Delivery d, Long transporterId) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 20px;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox idCol = new VBox(2);
        HBox.setHgrow(idCol, Priority.ALWAYS);
        Label title = new Label("Shipment #" + d.getTrackingCode());
        title.getStyleClass().add("heading-3");
        Label sub = new Label("Order #" + (d.getOrderNumber() != null ? d.getOrderNumber() : d.getOrderId()));
        sub.getStyleClass().add("subheading");
        idCol.getChildren().addAll(title, sub);

        Label badge = new Label(d.getStatus().getLabel());
        badge.getStyleClass().addAll("badge", "badge-info");
        topRow.getChildren().addAll(idCol, badge);

        // Details Grid
        HBox grid = new HBox(20);
        grid.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 12px 16px; -fx-background-radius: 8px;");
        
        String distStr = formatDistanceDisplay(d.getDistanceKm());
        String costStr = d.getDeliveryCost() != null ? String.format("₹%.2f", d.getDeliveryCost()) : "₹0.00";

        grid.getChildren().addAll(
            createDetailCell("Pickup Origin", d.getPickupLocation()),
            createDetailCell("Drop Destination", d.getDeliveryLocation()),
            createDetailCell("Route Distance", distStr),
            createDetailCell("Delivery Fee", costStr)
        );

        // Timeline Action Buttons
        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        switch (d.getStatus()) {
            case ACCEPTED -> {
                Button stepBtn = new Button("Mark Cargo Picked Up");
                stepBtn.getStyleClass().add("btn-primary");
                stepBtn.setOnAction(e -> handleAdvanceStatus(d, transporterId, DeliveryStatus.PICKED_UP));
                actionRow.getChildren().add(stepBtn);
            }
            case PICKED_UP -> {
                Button stepBtn = new Button("Mark In Transit");
                stepBtn.getStyleClass().add("btn-primary");
                stepBtn.setOnAction(e -> handleAdvanceStatus(d, transporterId, DeliveryStatus.IN_TRANSIT));
                actionRow.getChildren().add(stepBtn);
            }
            case IN_TRANSIT -> {
                Button stepBtn = new Button("Mark Delivered & Release Payout");
                stepBtn.getStyleClass().add("btn-primary");
                stepBtn.setOnAction(e -> handleAdvanceStatus(d, transporterId, DeliveryStatus.DELIVERED));
                actionRow.getChildren().add(stepBtn);
            }
            default -> {}
        }

        card.getChildren().addAll(topRow, grid, actionRow);
        AnimationHelper.setupCardHoverAnimation(card);
        return card;
    }

    private VBox createTrackingOnlyCard(Delivery d) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 18px 20px;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox idCol = new VBox(2);
        HBox.setHgrow(idCol, Priority.ALWAYS);
        Label title = new Label("Tracking #" + d.getTrackingCode());
        title.getStyleClass().add("heading-3");
        Label carrier = new Label("Carrier: " + (d.getTransporterName() != null ? d.getTransporterName() : "Assignment Pending"));
        carrier.getStyleClass().add("subheading");
        idCol.getChildren().addAll(title, carrier);

        Label badge = new Label(d.getStatus().getLabel());
        String badgeClass = d.getStatus() == DeliveryStatus.DELIVERED ? "badge-success" : "badge-info";
        badge.getStyleClass().addAll("badge", badgeClass);
        topRow.getChildren().addAll(idCol, badge);

        HBox grid = new HBox(20);
        grid.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 10px 14px; -fx-background-radius: 8px;");

        String distStr = formatDistanceDisplay(d.getDistanceKm());
        String costStr = d.getDeliveryCost() != null ? String.format("₹%.2f", d.getDeliveryCost()) : "₹0.00";

        grid.getChildren().addAll(
            createDetailCell("From", d.getPickupLocation()),
            createDetailCell("To", d.getDeliveryLocation()),
            createDetailCell("Distance", distStr),
            createDetailCell("Delivery Fee", costStr)
        );

        card.getChildren().addAll(topRow, grid);
        AnimationHelper.setupCardHoverAnimation(card);
        return card;
    }

    private String formatDistanceDisplay(BigDecimal dist) {
        if (dist == null) return "Distance unavailable";
        if (dist.compareTo(BigDecimal.ZERO) == 0) return "0.00 km (Same Location)";
        return String.format("%.2f km", dist);
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

    private void handleAcceptDelivery(Delivery d, Long transporterId) {
        CompactModal.showConfirmation(
            MainLayoutController.getInstance().getRootStackPane(),
            "Accept Delivery Assignment",
            "Are you sure you want to accept shipment #" + d.getTrackingCode() + "? You will be designated as the active carrier for this route.",
            "Accept Job",
            () -> {
                try {
                    deliveryService.acceptDeliveryRequest(transporterId, d.getId());
                    CompactModal.showSuccess(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Job Accepted",
                        "Shipment #" + d.getTrackingCode() + " has been added to your Active Deliveries. Proceed to the pickup location.",
                        () -> {
                            showAvailableRequestsTab = false;
                            updateTabStyles();
                            loadDeliveries();
                        }
                    );
                } catch (Exception e) {
                    logger.error("Failed to accept delivery", e);
                    CompactModal.showError(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Action Failed",
                        "Could not accept delivery: " + e.getMessage(),
                        null
                    );
                }
            },
            null
        );
    }

    private void handleAdvanceStatus(Delivery d, Long transporterId, DeliveryStatus newStatus) {
        if (newStatus == DeliveryStatus.DELIVERED) {
            CompactModal.showConfirmation(
                MainLayoutController.getInstance().getRootStackPane(),
                "Complete Delivery & Release Payout",
                "Mark shipment #" + d.getTrackingCode() + " as DELIVERED? This will complete the order and credit your freight earnings of ₹" + d.getDeliveryCost() + ".",
                "Confirm Delivery",
                () -> executeAdvanceStatus(d, transporterId, newStatus),
                null
            );
        } else {
            executeAdvanceStatus(d, transporterId, newStatus);
        }
    }

    private void executeAdvanceStatus(Delivery d, Long transporterId, DeliveryStatus newStatus) {
        try {
            deliveryService.advanceStatus(transporterId, d.getId(), newStatus);
            CompactModal.showSuccess(
                MainLayoutController.getInstance().getRootStackPane(),
                "Status Updated",
                "Shipment #" + d.getTrackingCode() + " milestone updated to: " + newStatus.getLabel(),
                this::loadDeliveries
            );
        } catch (Exception e) {
            logger.error("Failed to advance delivery status", e);
            CompactModal.showError(
                MainLayoutController.getInstance().getRootStackPane(),
                "Update Failed",
                "Could not advance status: " + e.getMessage(),
                null
            );
        }
    }
}
