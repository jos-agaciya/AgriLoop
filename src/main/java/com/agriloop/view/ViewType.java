package com.agriloop.view;

import org.kordamp.ikonli.feather.Feather;

/**
 * Route registry of all top-level application views.
 */
public enum ViewType {
    DASHBOARD("Dashboard", "Overview & key metrics", "/fxml/DashboardView.fxml", Feather.HOME),
    MARKETPLACE("Marketplace", "Browse agricultural waste listings", "/fxml/MarketplaceView.fxml", Feather.SHOPPING_BAG),
    SELL_WASTE("Sell Waste", "Create and publish a new waste listing", "/fxml/SellWasteView.fxml", Feather.PLUS_CIRCLE),
    MY_LISTINGS("My Listings", "Manage your listed agricultural waste", "/fxml/MyListingsView.fxml", Feather.PACKAGE),
    SALES_REQUESTS("Sales & Requests", "Incoming purchase requests and orders", "/fxml/SalesRequestsView.fxml", Feather.FILE_TEXT),
    ORDERS("My Orders", "Track purchase and procurement orders", "/fxml/OrdersView.fxml", Feather.CLIPBOARD),
    DELIVERIES("Track Deliveries", "Active logistics and transport tracking", "/fxml/DeliveriesView.fxml", Feather.TRUCK),
    DELIVERY_REQUESTS("Delivery Requests", "Available cargo dispatch requests", "/fxml/DeliveriesView.fxml", Feather.INBOX),
    ACTIVE_DELIVERIES("Active Deliveries", "In-transit shipment management", "/fxml/DeliveriesView.fxml", Feather.NAVIGATION),
    EARNINGS("Earnings", "Financial reports and haulage payouts", "/fxml/EarningsView.fxml", Feather.DOLLAR_SIGN),
    IMPACT("Impact & CO₂", "Sustainability analytics & carbon offsets", "/fxml/ImpactView.fxml", Feather.GLOBE),
    PROFILE("User Profile", "Account settings & stakeholder profile", "/fxml/ProfileView.fxml", Feather.USER);

    private final String title;
    private final String subtitle;
    private final String fxmlPath;
    private final Feather icon;

    ViewType(String title, String subtitle, String fxmlPath, Feather icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.fxmlPath = fxmlPath;
        this.icon = icon;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getFxmlPath() { return fxmlPath; }
    public Feather getIcon() { return icon; }
}

