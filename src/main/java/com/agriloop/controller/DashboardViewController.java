package com.agriloop.controller;

import com.agriloop.model.Delivery;
import com.agriloop.model.Order;
import com.agriloop.model.User;
import com.agriloop.model.WasteListing;
import com.agriloop.model.dashboard.FarmerDashboardData;
import com.agriloop.model.dashboard.ManufacturerDashboardData;
import com.agriloop.model.dashboard.TransporterDashboardData;
import com.agriloop.model.enums.UserRole;
import com.agriloop.service.DashboardService;
import com.agriloop.service.NavigationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.EmptyStateCard;
import com.agriloop.view.components.StatCard;
import javafx.application.Platform;
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
import java.util.ResourceBundle;

/**
 * Controller for the strict role-specific Dashboard Overview.
 * Renders tailored KPIs, role-specific actions, and real database activity for Farmer, Manufacturer, and Transporter.
 */
public class DashboardViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(DashboardViewController.class);

    @FXML private Label greetingHeaderLabel;
    @FXML private Label greetingSubtitleLabel;
    @FXML private Label kpiSectionTitle;
    @FXML private Label activitySectionTitle;
    @FXML private HBox statsContainer;
    @FXML private VBox recentActivityBox;

    @FXML private Button heroActionBtn1;
    @FXML private StackPane heroActionIconBox1;
    @FXML private Button heroActionBtn2;
    @FXML private StackPane heroActionIconBox2;
    @FXML private Button heroActionBtn3;
    @FXML private StackPane heroActionIconBox3;

    private final DashboardService dashboardService = DashboardService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadRoleDashboard();
    }

    private void loadRoleDashboard() {
        User user = ProfileService.getInstance().getCurrentUser();
        UserRole role = user != null ? user.getRole() : UserRole.FARMER;
        String userName = user != null ? user.getFullName() : "Member";
        Long userId = user != null ? user.getId() : null;

        greetingHeaderLabel.setText("Welcome back, " + userName);

        switch (role) {
            case FARMER -> setupFarmerDashboard(userId);
            case MANUFACTURER -> setupManufacturerDashboard(userId);
            case TRANSPORTER -> setupTransporterDashboard(userId);
            case ADMIN -> setupFarmerDashboard(userId);
        }
    }

    private void setupFarmerDashboard(Long userId) {
        greetingSubtitleLabel.setText("Monetize agricultural waste residues, manage incoming purchase requests, and track completed sales.");
        kpiSectionTitle.setText("Farmer Sales & Inventory Overview");
        activitySectionTitle.setText("Recent Listings & Incoming Requests");

        // Action 1: Sell Waste
        heroActionBtn1.setText("Sell Waste");
        heroActionBtn1.setVisible(true);
        heroActionBtn1.setManaged(true);
        setButtonIcon(heroActionIconBox1, Feather.PLUS_CIRCLE, "#FFFFFF");
        heroActionBtn1.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.SELL_WASTE));

        // Action 2: My Listings
        heroActionBtn2.setText("My Listings");
        heroActionBtn2.setVisible(true);
        heroActionBtn2.setManaged(true);
        setButtonIcon(heroActionIconBox2, Feather.PACKAGE, "#1E293B");
        heroActionBtn2.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.MY_LISTINGS));

        // Action 3: Sales & Requests
        heroActionBtn3.setText("Sales & Requests");
        heroActionBtn3.setVisible(true);
        heroActionBtn3.setManaged(true);
        setButtonIcon(heroActionIconBox3, Feather.FILE_TEXT, "#1E293B");
        heroActionBtn3.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.SALES_REQUESTS));

        // Live Real Database Metrics
        FarmerDashboardData data = dashboardService.getFarmerDashboardData(userId);

        statsContainer.getChildren().clear();
        StatCard card1 = new StatCard(Feather.PACKAGE, "Active Listings", String.valueOf(data.getActiveListingsCount()), "Live in marketplace", StatCard.StatTheme.EMERALD);
        StatCard card2 = new StatCard(Feather.LAYERS, "Waste Available", String.format("%.2f Tons", data.getWasteAvailableTons()), "Unsold biomass stock", StatCard.StatTheme.BLUE);
        StatCard card3 = new StatCard(Feather.FILE_TEXT, "Pending Requests", String.valueOf(data.getPendingRequestsCount()), "Awaiting your review", StatCard.StatTheme.AMBER);
        StatCard card4 = new StatCard(Feather.CHECK_CIRCLE, "Completed Sales", String.valueOf(data.getCompletedSalesCount()), "Fulfilled orders", StatCard.StatTheme.PURPLE);
        StatCard card5 = new StatCard(Feather.DOLLAR_SIGN, "Total Earnings", String.format("₹%.2f", data.getTotalEarnings()), "Settled payments", StatCard.StatTheme.EMERALD);

        statsContainer.getChildren().addAll(card1, card2, card3, card4, card5);

        // Activity Feed
        recentActivityBox.getChildren().clear();
        if (data.getRecentListings().isEmpty() && data.getIncomingRequests().isEmpty()) {
            EmptyStateCard emptyCard = new EmptyStateCard(
                Feather.PACKAGE,
                "No listings or sales activity yet",
                "Create an agricultural waste listing to start monetizing crop residues and receive direct purchase orders.",
                "Sell Waste",
                () -> NavigationService.getInstance().navigateTo(ViewType.SELL_WASTE)
            );
            recentActivityBox.getChildren().add(emptyCard);
        } else {
            for (WasteListing listing : data.getRecentListings()) {
                recentActivityBox.getChildren().add(createActivityRow(
                    Feather.PACKAGE,
                    listing.getTitle(),
                    listing.getWasteType() + " • " + listing.getQuantityTons() + " Tons @ ₹" + listing.getPricePerTon() + "/Ton",
                    listing.getStatus().getLabel(),
                    "badge-success"
                ));
            }
            for (Order order : data.getIncomingRequests()) {
                recentActivityBox.getChildren().add(createActivityRow(
                    Feather.SHOPPING_BAG,
                    "Request #" + order.getOrderNumber(),
                    "Buyer: " + (order.getBuyerName() != null ? order.getBuyerName() : "Procurement Buyer") + " • Amount: ₹" + order.getTotalAmount(),
                    order.getStatus().getLabel(),
                    "badge-info"
                ));
            }
        }
    }

    private void setupManufacturerDashboard(Long userId) {
        greetingSubtitleLabel.setText("Discover agricultural materials and manage your procurement.");
        kpiSectionTitle.setText("Procurement & Sourcing Overview");
        activitySectionTitle.setText("Recent Purchases & Supply Pipeline");

        // Action 1: Browse Materials
        heroActionBtn1.setText("Browse Materials");
        heroActionBtn1.setVisible(true);
        heroActionBtn1.setManaged(true);
        setButtonIcon(heroActionIconBox1, Feather.SHOPPING_BAG, "#FFFFFF");
        heroActionBtn1.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.MARKETPLACE));

        // Action 2: My Orders
        heroActionBtn2.setText("My Orders");
        heroActionBtn2.setVisible(true);
        heroActionBtn2.setManaged(true);
        setButtonIcon(heroActionIconBox2, Feather.CLIPBOARD, "#1E293B");
        heroActionBtn2.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.ORDERS));

        // Action 3: Deliveries
        heroActionBtn3.setText("Deliveries");
        heroActionBtn3.setVisible(true);
        heroActionBtn3.setManaged(true);
        setButtonIcon(heroActionIconBox3, Feather.TRUCK, "#1E293B");
        heroActionBtn3.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.DELIVERIES));

        // Live Real Database Metrics
        ManufacturerDashboardData data = dashboardService.getManufacturerDashboardData(userId);

        statsContainer.getChildren().clear();
        StatCard card1 = new StatCard(Feather.PACKAGE, "Materials Purchased", String.format("%.2f Tons", data.getMaterialsPurchasedTons()), "Procured biomass", StatCard.StatTheme.BLUE);
        StatCard card2 = new StatCard(Feather.CLIPBOARD, "Active Orders", String.valueOf(data.getActiveOrdersCount()), "In pipeline", StatCard.StatTheme.AMBER);
        StatCard card3 = new StatCard(Feather.TRUCK, "Pending Deliveries", String.valueOf(data.getPendingDeliveriesCount()), "En route / Pending", StatCard.StatTheme.BLUE);
        StatCard card4 = new StatCard(Feather.DOLLAR_SIGN, "Total Spent", String.format("₹%.2f", data.getTotalSpent()), "Procurement value", StatCard.StatTheme.PURPLE);

        statsContainer.getChildren().addAll(card1, card2, card3, card4);

        // Activity Feed
        recentActivityBox.getChildren().clear();
        if (data.getRecentOrders().isEmpty()) {
            EmptyStateCard emptyCard = new EmptyStateCard(
                Feather.SHOPPING_BAG,
                "No procurement orders placed yet",
                "Explore available crop residues, straws, husks, and bio-waste on the marketplace catalog and place purchase requests.",
                "Browse Materials",
                () -> NavigationService.getInstance().navigateTo(ViewType.MARKETPLACE)
            );
            recentActivityBox.getChildren().add(emptyCard);
        } else {
            for (Order order : data.getRecentOrders()) {
                recentActivityBox.getChildren().add(createActivityRow(
                    Feather.SHOPPING_BAG,
                    "Order #" + order.getOrderNumber(),
                    "Seller: " + (order.getSellerName() != null ? order.getSellerName() : "Seller") + " • Amount: ₹" + order.getTotalAmount(),
                    order.getStatus().getLabel(),
                    "badge-info"
                ));
            }
        }
    }

    private void setupTransporterDashboard(Long userId) {
        greetingSubtitleLabel.setText("Manage delivery assignments and track your active routes.");
        kpiSectionTitle.setText("Logistics & Dispatch Performance");
        activitySectionTitle.setText("Active Deliveries & Cargo Requests");

        // Action 1: Delivery Requests
        heroActionBtn1.setText("Delivery Requests");
        heroActionBtn1.setVisible(true);
        heroActionBtn1.setManaged(true);
        setButtonIcon(heroActionIconBox1, Feather.INBOX, "#FFFFFF");
        heroActionBtn1.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.DELIVERY_REQUESTS));

        // Action 2: Active Deliveries
        heroActionBtn2.setText("Active Deliveries");
        heroActionBtn2.setVisible(true);
        heroActionBtn2.setManaged(true);
        setButtonIcon(heroActionIconBox2, Feather.TRUCK, "#1E293B");
        heroActionBtn2.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.ACTIVE_DELIVERIES));

        // Action 3: Earnings
        heroActionBtn3.setText("Earnings");
        heroActionBtn3.setVisible(true);
        heroActionBtn3.setManaged(true);
        setButtonIcon(heroActionIconBox3, Feather.DOLLAR_SIGN, "#1E293B");
        heroActionBtn3.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.EARNINGS));

        // Live Real Database Metrics
        TransporterDashboardData data = dashboardService.getTransporterDashboardData(userId);

        statsContainer.getChildren().clear();
        StatCard card1 = new StatCard(Feather.INBOX, "Pending Delivery Requests", String.valueOf(data.getPendingRequestsCount()), "Available dispatches", StatCard.StatTheme.AMBER);
        StatCard card2 = new StatCard(Feather.TRUCK, "Active Deliveries", String.valueOf(data.getActiveDeliveriesCount()), "Currently in transit", StatCard.StatTheme.BLUE);
        StatCard card3 = new StatCard(Feather.CHECK_CIRCLE, "Completed Deliveries", String.valueOf(data.getCompletedDeliveriesCount()), "Fulfilled haulage", StatCard.StatTheme.EMERALD);
        StatCard card4 = new StatCard(Feather.DOLLAR_SIGN, "Total Earnings", String.format("₹%.2f", data.getTotalEarnings()), "Freight payouts", StatCard.StatTheme.EMERALD);
        StatCard card5 = new StatCard(Feather.MAP_PIN, "Distance Covered", String.format("%.1f km", data.getDistanceCoveredKm()), "Transport mileage", StatCard.StatTheme.PURPLE);

        statsContainer.getChildren().addAll(card1, card2, card3, card4, card5);

        // Activity Feed
        recentActivityBox.getChildren().clear();
        if (data.getRecentDeliveries().isEmpty()) {
            EmptyStateCard emptyCard = new EmptyStateCard(
                Feather.TRUCK,
                "No delivery assignments yet",
                "Available cargo transport requests and route dispatches will appear here for pickup.",
                "Delivery Requests",
                () -> NavigationService.getInstance().navigateTo(ViewType.DELIVERY_REQUESTS)
            );
            recentActivityBox.getChildren().add(emptyCard);
        } else {
            for (Delivery delivery : data.getRecentDeliveries()) {
                recentActivityBox.getChildren().add(createActivityRow(
                    Feather.TRUCK,
                    "Tracking: " + delivery.getTrackingCode(),
                    delivery.getPickupLocation() + " → " + delivery.getDeliveryLocation() + " • Cost: ₹" + delivery.getDeliveryCost(),
                    delivery.getStatus().getLabel(),
                    "badge-neutral"
                ));
            }
        }
    }

    private void setButtonIcon(StackPane iconBox, Feather feather, String colorHex) {
        iconBox.getChildren().clear();
        FontIcon icon = IconHelper.createIcon(feather, 16);
        icon.setStyle("-fx-icon-color: " + colorHex + ";");
        iconBox.getChildren().add(icon);
    }

    private HBox createActivityRow(Feather icon, String title, String subtitle, String statusText, String statusClass) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        row.setStyle("-fx-padding: 12px 18px; -fx-background-radius: 10px;");
        HBox.setHgrow(row, Priority.ALWAYS);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 8px; -fx-padding: 8px;");
        FontIcon fontIcon = IconHelper.createIcon(icon, 18);
        fontIcon.setStyle("-fx-icon-color: #475569;");
        iconBox.getChildren().add(fontIcon);

        VBox textCol = new VBox(2);
        HBox.setHgrow(textCol, Priority.ALWAYS);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: -fx-color-text-primary;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-color-text-secondary;");
        textCol.getChildren().addAll(titleLbl, subLbl);

        Label badge = new Label(statusText);
        badge.getStyleClass().addAll("badge", statusClass);

        row.getChildren().addAll(iconBox, textCol, badge);
        AnimationHelper.setupCardHoverAnimation(row);
        return row;
    }
}

