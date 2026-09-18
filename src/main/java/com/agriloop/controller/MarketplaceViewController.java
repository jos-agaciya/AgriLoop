package com.agriloop.controller;

import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.User;
import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.UserRole;
import com.agriloop.model.enums.WasteCategory;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.WasteListingRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import com.agriloop.repository.impl.JdbcWasteListingRepository;
import com.agriloop.service.NavigationService;
import com.agriloop.service.OrderService;
import com.agriloop.service.ProfileService;
import com.agriloop.service.intelligence.SmartWasteMatchingService;
import com.agriloop.service.intelligence.ValueEstimationService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import com.agriloop.util.ValidationUtil;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.CompactModal;
import com.agriloop.view.components.EmptyStateCard;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Marketplace Discovery & Procurement View with structured delivery address.
 */
public class MarketplaceViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MarketplaceViewController.class);

    @FXML private TextField searchField;
    @FXML private ComboBox<WasteCategory> categoryFilterCombo;
    @FXML private Button filterApplyBtn;
    @FXML private Button refreshBtn;
    @FXML private StackPane refreshIconBox;
    @FXML private VBox listingsContainer;
    @FXML private Label countLabel;

    // Detail & Purchase Modal Controls
    @FXML private StackPane detailModalOverlay;
    @FXML private Label modalTitleLabel;
    @FXML private Label modalSubtitleLabel;
    @FXML private Button modalCloseBtn;
    @FXML private Label modalCategoryLabel;
    @FXML private Label modalQuantityLabel;
    @FXML private Label modalPriceLabel;
    @FXML private Label modalMoistureLabel;
    @FXML private Label modalLocationLabel;
    @FXML private Label modalSellerLabel;
    @FXML private Label modalDescriptionLabel;
    @FXML private VBox modalMatchingContainer;
    @FXML private Label modalValuationRangeLabel;
    @FXML private Label modalValuationDemandLabel;

    @FXML private TextField orderQtyField;
    @FXML private TextField orderTotalField;

    // Structured Delivery Address Fields
    @FXML private TextField orderHouseNoField;
    @FXML private TextField orderStreetField;
    @FXML private TextField orderAreaField;
    @FXML private TextField orderCityField;
    @FXML private TextField orderDistrictField;
    @FXML private TextField orderStateField;
    @FXML private TextField orderPinField;
    @FXML private TextField orderCountryField;

    @FXML private TextField orderNotesField;
    @FXML private Label orderErrorLabel;
    @FXML private Button modalCancelBtn;
    @FXML private Button modalSubmitOrderBtn;

    private final WasteListingRepository listingRepo = new JdbcWasteListingRepository();
    private final UserRepository userRepo = new JdbcUserRepository();
    private final OrderService orderService = OrderService.getInstance();
    private final SmartWasteMatchingService matchingService = SmartWasteMatchingService.getInstance();
    private final ValueEstimationService valuationService = ValueEstimationService.getInstance();

    private WasteListing activeSelectedListing;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon refreshIcon = IconHelper.createIcon(Feather.REFRESH_CW, 14);
        refreshIcon.setStyle("-fx-icon-color: #475569;");
        refreshIconBox.getChildren().add(refreshIcon);

        FontIcon closeXIcon = IconHelper.createIcon(Feather.X, 15);
        closeXIcon.setStyle("-fx-icon-color: #64748B;");
        modalCloseBtn.setGraphic(closeXIcon);
        modalCloseBtn.setText("");

        setupCategoryFilter();

        String pendingQuery = NavigationService.getInstance().getPendingSearchQuery();
        if (pendingQuery != null && !pendingQuery.isBlank()) {
            searchField.setText(pendingQuery);
        }

        loadListings();

        filterApplyBtn.setOnAction(e -> loadListings());
        refreshBtn.setOnAction(e -> loadListings());
        searchField.setOnAction(e -> loadListings());

        modalCloseBtn.setOnAction(e -> closeDetailModal());
        modalCancelBtn.setOnAction(e -> closeDetailModal());
        modalSubmitOrderBtn.setOnAction(e -> handlePlaceOrder());

        // Live quantity calculation listener
        orderQtyField.textProperty().addListener((obs, oldVal, newVal) -> updateOrderTotalCalculation());
    }

    private void setupCategoryFilter() {
        categoryFilterCombo.getItems().clear();
        categoryFilterCombo.getItems().add(null);
        categoryFilterCombo.getItems().addAll(WasteCategory.values());

        categoryFilterCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WasteCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Categories" : item.getDisplayName());
            }
        });

        categoryFilterCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WasteCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All Categories" : item.getDisplayName());
            }
        });
    }

    private void loadListings() {
        listingsContainer.getChildren().clear();
        String keyword = searchField.getText() != null ? searchField.getText().trim() : null;
        WasteCategory category = categoryFilterCombo.getValue();

        List<WasteListing> results = List.of();
        try {
            results = listingRepo.searchAvailableListings(keyword, category);
        } catch (Exception e) {
            logger.error("Failed to fetch marketplace listings", e);
        }

        countLabel.setText(results.size() + " materials available");

        if (results.isEmpty()) {
            EmptyStateCard emptyState = new EmptyStateCard(
                Feather.SEARCH,
                "No agricultural materials available right now",
                "There are currently no active biomass materials matching your filter criteria. New crop residue listings from farmers will appear here.",
                "Refresh Catalog",
                this::loadListings
            );
            listingsContainer.getChildren().add(emptyState);
        } else {
            for (WasteListing listing : results) {
                listingsContainer.getChildren().add(createListingCard(listing));
            }
        }
    }

    private HBox createListingCard(WasteListing listing) {
        HBox card = new HBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 18px 22px; -fx-cursor: hand;");

        // Icon Box
        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color: #ECFDF5; -fx-background-radius: 10px; -fx-padding: 12px;");
        FontIcon icon = IconHelper.createIcon(Feather.PACKAGE, 24);
        icon.setStyle("-fx-icon-color: #059669;");
        iconBox.getChildren().add(icon);

        // Information Column
        VBox infoCol = new VBox(4);
        HBox.setHgrow(infoCol, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(listing.getTitle());
        title.getStyleClass().add("heading-3");

        Label catBadge = new Label(listing.getCategory().getDisplayName());
        catBadge.getStyleClass().addAll("badge", "badge-neutral");
        titleRow.getChildren().addAll(title, catBadge);

        String meta = String.format("%s • %s • Farm Location: %s", 
            listing.getWasteType(),
            listing.getFarmerName() != null ? "Listed by " + listing.getFarmerName() : "Verified Farmer",
            listing.getLocation());
        Label metaLbl = new Label(meta);
        metaLbl.getStyleClass().add("subheading");

        infoCol.getChildren().addAll(titleRow, metaLbl);

        // Pricing & Quantity Stats
        VBox statsCol = new VBox(2);
        statsCol.setAlignment(Pos.CENTER_RIGHT);
        Label priceLbl = new Label(String.format("₹%.2f / Ton", listing.getPricePerTon()));
        priceLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label qtyLbl = new Label(String.format("%.2f Tons Available", listing.getQuantityTons()));
        qtyLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #059669; -fx-font-weight: 500;");
        statsCol.getChildren().addAll(priceLbl, qtyLbl);

        // Action Button
        Button detailsBtn = new Button("View & Purchase");
        detailsBtn.getStyleClass().add("btn-primary");
        detailsBtn.setOnAction(e -> openDetailModal(listing));

        card.getChildren().addAll(iconBox, infoCol, statsCol, detailsBtn);
        card.setOnMouseClicked(e -> openDetailModal(listing));
        AnimationHelper.setupCardHoverAnimation(card);
        return card;
    }

    private void openDetailModal(WasteListing listing) {
        this.activeSelectedListing = listing;
        orderErrorLabel.setText("");

        modalTitleLabel.setText(listing.getTitle());
        modalSubtitleLabel.setText(listing.getWasteType() + " • Origin: " + listing.getLocation());
        modalCategoryLabel.setText(listing.getCategory().getDisplayName());
        modalQuantityLabel.setText(String.format("%.2f Tons", listing.getQuantityTons()));
        modalPriceLabel.setText(String.format("₹%.2f / Ton", listing.getPricePerTon()));
        modalMoistureLabel.setText(listing.getMoistureContentPct() != null 
            ? String.format("%.1f%%", listing.getMoistureContentPct()) : "Standard");
        modalLocationLabel.setText(listing.getLocation());
        modalSellerLabel.setText(listing.getFarmerName() != null ? listing.getFarmerName() : "Registered Farmer");
        modalDescriptionLabel.setText(listing.getDescription() != null && !listing.getDescription().isBlank() 
            ? listing.getDescription() : "High-quality agricultural byproduct suitable for bio-processing and clean energy.");

        // Smart Waste Matching
        modalMatchingContainer.getChildren().clear();
        List<SmartWasteMatchingService.IndustrialMatch> matches = matchingService.findMatches(listing);
        for (var m : matches) {
            HBox matchRow = new HBox(8);
            matchRow.setAlignment(Pos.CENTER_LEFT);
            Label badge = new Label(m.suitabilityScore() + "% Match");
            badge.getStyleClass().addAll("badge", "badge-success");
            Label desc = new Label(m.applicationTitle() + " (" + m.targetIndustry() + ")");
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803D; -fx-font-weight: 500;");
            matchRow.getChildren().addAll(badge, desc);
            modalMatchingContainer.getChildren().add(matchRow);
        }

        // Value Estimation
        ValueEstimationService.ValuationResult val = valuationService.estimateValue(listing);
        modalValuationRangeLabel.setText(String.format("₹%.2f - ₹%.2f / Ton", val.estimatedPricePerTonMin(), val.estimatedPricePerTonMax()));
        modalValuationDemandLabel.setText("Market Demand: " + val.demandLevel() + " • " + (val.pricingFactors().isEmpty() ? "Standard" : val.pricingFactors().get(0)));

        // Order Form Reset
        orderQtyField.setText(listing.getQuantityTons().toString());
        orderNotesField.clear();
        orderCountryField.setText("India");

        // Prefill Delivery Address if Buyer Profile exists
        User current = ProfileService.getInstance().getCurrentUser();
        if (current != null) {
            try {
                Optional<ManufacturerProfile> mp = userRepo.findManufacturerProfile(current.getId());
                mp.ifPresent(p -> {
                    String existingAddr = p.getFacilityAddress();
                    if (existingAddr != null && !existingAddr.isBlank()) {
                        String[] parts = existingAddr.split(",");
                        if (parts.length >= 1 && orderHouseNoField.getText().isBlank()) orderHouseNoField.setText(parts[0].trim());
                        if (parts.length >= 2 && orderStreetField.getText().isBlank()) orderStreetField.setText(parts[1].trim());
                        if (parts.length >= 3 && orderCityField.getText().isBlank()) orderCityField.setText(parts[2].trim());
                    }
                });
            } catch (Exception ignored) {}
        }

        updateOrderTotalCalculation();

        detailModalOverlay.setVisible(true);
        detailModalOverlay.setManaged(true);
        AnimationHelper.fadeIn(detailModalOverlay, null, null);
    }

    private void updateOrderTotalCalculation() {
        if (activeSelectedListing == null) return;
        String qtyText = orderQtyField.getText() != null ? orderQtyField.getText().trim() : "";
        try {
            BigDecimal qty = new BigDecimal(qtyText);
            if (qty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal total = qty.multiply(activeSelectedListing.getPricePerTon()).setScale(2, RoundingMode.HALF_UP);
                orderTotalField.setText(String.format("₹%.2f", total));
            } else {
                orderTotalField.setText("₹0.00");
            }
        } catch (Exception e) {
            orderTotalField.setText("₹0.00");
        }
    }

    private void handlePlaceOrder() {
        orderErrorLabel.setText("");
        User current = ProfileService.getInstance().getCurrentUser();
        if (current == null || current.getId() == null) {
            orderErrorLabel.setText("Please sign in as a Manufacturer / Buyer to submit a purchase request.");
            return;
        }

        if (current.getRole() != UserRole.MANUFACTURER && current.getRole() != UserRole.ADMIN) {
            orderErrorLabel.setText("Only registered Manufacturers / Buyers can place purchase requests.");
            return;
        }

        String qtyStr = orderQtyField.getText() != null ? orderQtyField.getText().trim() : "";
        String notes = orderNotesField.getText() != null ? orderNotesField.getText().trim() : "";

        if (qtyStr.isBlank() || !ValidationUtil.isPositiveDecimal(qtyStr)) {
            orderErrorLabel.setText("Please enter a valid requested quantity in tons.");
            return;
        }

        // Structured Delivery Address Validation
        String houseNo = orderHouseNoField.getText();
        String street = orderStreetField.getText();
        String area = orderAreaField.getText();
        String city = orderCityField.getText();
        String district = orderDistrictField.getText();
        String state = orderStateField.getText();
        String pinCode = orderPinField.getText();
        String country = orderCountryField.getText();

        String addressError = ValidationUtil.validateStructuredAddress(
            houseNo, street, area, city, district, state, pinCode, country
        );

        if (addressError != null) {
            orderErrorLabel.setText(addressError);
            return;
        }

        ValidationUtil.StructuredAddress structuredAddr = new ValidationUtil.StructuredAddress(
            houseNo, street, area, city, district, state, pinCode, country
        );
        String canonicalAddress = structuredAddr.toCanonicalAddress();

        try {
            BigDecimal qty = new BigDecimal(qtyStr);
            orderService.placePurchaseRequest(current.getId(), activeSelectedListing.getId(), qty, canonicalAddress, notes);
            closeDetailModal();

            CompactModal.showSuccess(
                MainLayoutController.getInstance().getRootStackPane(),
                "Purchase Request Sent",
                "Your purchase request for " + qty + " Tons of '" + activeSelectedListing.getTitle() + 
                "' was submitted to the seller. Delivery routing to " + area + ", " + city + " has been registered.",
                () -> NavigationService.getInstance().navigateTo(ViewType.ORDERS)
            );

        } catch (Exception e) {
            logger.error("Failed to place purchase request", e);
            orderErrorLabel.setText(e.getMessage());
        }
    }

    private void closeDetailModal() {
        detailModalOverlay.setVisible(false);
        detailModalOverlay.setManaged(false);
        activeSelectedListing = null;
    }
}
