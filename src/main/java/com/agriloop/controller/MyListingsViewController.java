package com.agriloop.controller;

import com.agriloop.model.User;
import com.agriloop.model.WasteListing;
import com.agriloop.service.ListingService;
import com.agriloop.service.NavigationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.AnimationHelper;
import com.agriloop.util.IconHelper;
import com.agriloop.util.ValidationUtil;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.CompactModal;
import com.agriloop.view.components.EmptyStateCard;
import com.agriloop.view.components.ToastNotification;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Farmer's My Listings management view.
 */
public class MyListingsViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(MyListingsViewController.class);

    @FXML private VBox listingsContainer;
    @FXML private Button createNewBtn;
    @FXML private StackPane addIconContainer;

    // Edit Modal
    @FXML private StackPane editModalOverlay;
    @FXML private Button editModalCloseBtn;
    @FXML private TextField editTitleField;
    @FXML private TextField editQuantityField;
    @FXML private TextField editPriceField;
    @FXML private TextField editLocationField;
    @FXML private TextArea editDescriptionArea;
    @FXML private Label editErrorLabel;
    @FXML private Button editModalCancelBtn;
    @FXML private Button editModalSaveBtn;

    private final ListingService listingService = ListingService.getInstance();
    private WasteListing editingListing;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon icon = IconHelper.createIcon(Feather.PLUS, 15);
        icon.setStyle("-fx-icon-color: #FFFFFF;");
        addIconContainer.getChildren().add(icon);

        FontIcon closeXIcon = IconHelper.createIcon(Feather.X, 15);
        closeXIcon.setStyle("-fx-icon-color: #64748B;");
        editModalCloseBtn.setGraphic(closeXIcon);
        editModalCloseBtn.setText("");

        createNewBtn.setOnAction(e -> NavigationService.getInstance().navigateTo(ViewType.SELL_WASTE));
        editModalCloseBtn.setOnAction(e -> closeEditModal());
        editModalCancelBtn.setOnAction(e -> closeEditModal());
        editModalSaveBtn.setOnAction(e -> handleSaveEdit());

        loadListings();
    }

    private void loadListings() {
        listingsContainer.getChildren().clear();
        User current = ProfileService.getInstance().getCurrentUser();
        if (current == null || current.getId() == null) {
            listingsContainer.getChildren().add(new EmptyStateCard(
                Feather.LIST,
                "Authentication Required",
                "Please sign in as a Farmer to view your listings.",
                "Sign In",
                () -> {}
            ));
            return;
        }

        List<WasteListing> list = List.of();
        try {
            list = listingService.getListingsByFarmer(current.getId());
        } catch (Exception e) {
            logger.error("Failed to load listings for farmer {}", current.getId(), e);
        }

        if (list.isEmpty()) {
            EmptyStateCard emptyState = new EmptyStateCard(
                Feather.LIST,
                "No agricultural waste listings found",
                "Your listings will appear here once you add agricultural waste. Create a listing to monetize crop residues and connect with buyers.",
                "Sell Waste",
                () -> NavigationService.getInstance().navigateTo(ViewType.SELL_WASTE)
            );
            listingsContainer.getChildren().add(emptyState);
        } else {
            for (WasteListing listing : list) {
                listingsContainer.getChildren().add(createFarmerListingCard(listing, current.getId()));
            }
        }
    }

    private HBox createFarmerListingCard(WasteListing listing, Long farmerId) {
        HBox card = new HBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 18px 22px;");

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

        Label statusBadge = new Label(listing.getStatus().getLabel());
        String badgeClass = switch (listing.getStatus()) {
            case AVAILABLE -> "badge-success";
            case RESERVED -> "badge-warning";
            case SOLD_OUT -> "badge-neutral";
            case EXPIRED, CANCELLED -> "badge-danger";
        };
        statusBadge.getStyleClass().addAll("badge", badgeClass);
        titleRow.getChildren().addAll(title, statusBadge);

        String meta = String.format("%s • Category: %s • Farm Location: %s", 
            listing.getWasteType(), listing.getCategory().getDisplayName(), listing.getLocation());
        Label metaLbl = new Label(meta);
        metaLbl.getStyleClass().add("subheading");

        infoCol.getChildren().addAll(titleRow, metaLbl);

        // Pricing & Quantity Stats
        VBox statsCol = new VBox(2);
        statsCol.setAlignment(Pos.CENTER_RIGHT);
        Label priceLbl = new Label(String.format("₹%.2f / Ton", listing.getPricePerTon()));
        priceLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label qtyLbl = new Label(String.format("%.2f Tons in Stock", listing.getQuantityTons()));
        qtyLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #059669; -fx-font-weight: 500;");
        statsCol.getChildren().addAll(priceLbl, qtyLbl);

        // Action Buttons
        HBox actionBox = new HBox(8);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> openEditModal(listing));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-secondary");
        deleteBtn.setStyle("-fx-text-fill: #DC2626; -fx-border-color: #FECACA;");
        deleteBtn.setOnAction(e -> handleDelete(listing, farmerId));

        actionBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(iconBox, infoCol, statsCol, actionBox);
        AnimationHelper.setupCardHoverAnimation(card);
        return card;
    }

    private void openEditModal(WasteListing listing) {
        this.editingListing = listing;
        editErrorLabel.setText("");

        editTitleField.setText(listing.getTitle());
        editQuantityField.setText(listing.getQuantityTons().toString());
        editPriceField.setText(listing.getPricePerTon().toString());
        editLocationField.setText(listing.getLocation());
        editDescriptionArea.setText(listing.getDescription() != null ? listing.getDescription() : "");

        editModalOverlay.setVisible(true);
        editModalOverlay.setManaged(true);
        AnimationHelper.fadeIn(editModalOverlay, null, null);
    }

    private void closeEditModal() {
        editModalOverlay.setVisible(false);
        editModalOverlay.setManaged(false);
        editingListing = null;
    }

    private void handleSaveEdit() {
        if (editingListing == null) return;
        editErrorLabel.setText("");

        String title = editTitleField.getText() != null ? editTitleField.getText().trim() : "";
        String qtyStr = editQuantityField.getText() != null ? editQuantityField.getText().trim() : "";
        String priceStr = editPriceField.getText() != null ? editPriceField.getText().trim() : "";
        String location = editLocationField.getText() != null ? editLocationField.getText().trim() : "";
        String description = editDescriptionArea.getText() != null ? editDescriptionArea.getText().trim() : "";

        if (title.isBlank() || qtyStr.isBlank() || priceStr.isBlank() || location.isBlank()) {
            editErrorLabel.setText("Please fill in all required fields.");
            return;
        }

        if (!ValidationUtil.isPositiveDecimal(qtyStr) || !ValidationUtil.isPositiveDecimal(priceStr)) {
            editErrorLabel.setText("Quantity and Price must be positive numbers.");
            return;
        }

        try {
            editingListing.setTitle(title);
            editingListing.setQuantityTons(new BigDecimal(qtyStr));
            editingListing.setPricePerTon(new BigDecimal(priceStr));
            editingListing.setLocation(location);
            editingListing.setDescription(description);

            listingService.saveListing(editingListing);
            closeEditModal();

            CompactModal.showSuccess(
                MainLayoutController.getInstance().getRootStackPane(),
                "Listing Updated",
                "Listing '" + title + "' was updated successfully.",
                this::loadListings
            );
        } catch (Exception e) {
            logger.error("Failed to update listing", e);
            editErrorLabel.setText(e.getMessage());
        }
    }

    private void handleDelete(WasteListing listing, Long farmerId) {
        CompactModal.showConfirmation(
            MainLayoutController.getInstance().getRootStackPane(),
            "Remove Listing",
            "Are you sure you want to remove '" + listing.getTitle() + "' from the marketplace catalog?",
            "Remove Listing",
            () -> {
                try {
                    listingService.safeDeleteListing(farmerId, listing.getId());
                    CompactModal.showSuccess(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Listing Removed",
                        "Listing '" + listing.getTitle() + "' was successfully removed.",
                        this::loadListings
                    );
                } catch (Exception e) {
                    logger.warn("Could not delete listing", e);
                    CompactModal.showError(
                        MainLayoutController.getInstance().getRootStackPane(),
                        "Delete Failed",
                        e.getMessage(),
                        null
                    );
                }
            },
            null
        );
    }
}
