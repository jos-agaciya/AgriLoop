package com.agriloop.controller;

import com.agriloop.model.User;
import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.ListingStatus;
import com.agriloop.model.enums.UserRole;
import com.agriloop.model.enums.WasteCategory;
import com.agriloop.repository.WasteListingRepository;
import com.agriloop.repository.impl.JdbcWasteListingRepository;
import com.agriloop.service.NavigationService;
import com.agriloop.service.NotificationService;
import com.agriloop.service.ProfileService;
import com.agriloop.util.IconHelper;
import com.agriloop.util.ValidationUtil;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.CompactModal;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller for creating and publishing new agricultural waste listings with structured address validation.
 */
public class SellWasteViewController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(SellWasteViewController.class);

    @FXML private StackPane formHeaderIconContainer;
    @FXML private TextField titleField;
    @FXML private TextField wasteTypeField;
    @FXML private ComboBox<WasteCategory> categoryCombo;
    @FXML private TextField quantityField;
    @FXML private TextField priceField;
    @FXML private TextField moistureField;

    // Structured Farm Pickup Location Fields
    @FXML private TextField houseNoField;
    @FXML private TextField streetField;
    @FXML private TextField areaField;
    @FXML private TextField cityField;
    @FXML private TextField districtField;
    @FXML private TextField stateField;
    @FXML private TextField pinCodeField;
    @FXML private TextField countryField;

    @FXML private DatePicker availableFromPicker;
    @FXML private DatePicker availableUntilPicker;
    @FXML private TextArea descriptionArea;
    @FXML private Button publishButton;
    @FXML private Button resetButton;
    @FXML private Label validationStatusLabel;

    private final WasteListingRepository listingRepo = new JdbcWasteListingRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon icon = IconHelper.createIcon(Feather.PLUS_CIRCLE, 22);
        icon.setStyle("-fx-icon-color: #059669;");
        formHeaderIconContainer.getChildren().add(icon);

        categoryCombo.getItems().addAll(WasteCategory.values());
        categoryCombo.setValue(WasteCategory.CROP_RESIDUE);
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(WasteCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });
        categoryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(WasteCategory item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getDisplayName());
            }
        });

        availableFromPicker.setValue(LocalDate.now());
        availableUntilPicker.setValue(LocalDate.now().plusMonths(1));
        countryField.setText("India");

        publishButton.setOnAction(e -> handlePublish());
        resetButton.setOnAction(e -> handleReset());
    }

    private void handlePublish() {
        validationStatusLabel.setText("");

        User currentUser = ProfileService.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null || currentUser.getRole() != UserRole.FARMER) {
            validationStatusLabel.setText("Please sign in as a Farmer / Seller to publish a listing.");
            return;
        }

        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        String wasteType = wasteTypeField.getText() != null ? wasteTypeField.getText().trim() : "";
        String qtyStr = quantityField.getText() != null ? quantityField.getText().trim() : "";
        String priceStr = priceField.getText() != null ? priceField.getText().trim() : "";

        if (title.isBlank() || wasteType.isBlank()) {
            validationStatusLabel.setText("Please fill in Title and Waste / Byproduct Type.");
            return;
        }

        String cleanQty = qtyStr.replace(",", "").trim();
        String cleanPrice = priceStr.replace("₹", "").replace("$", "").replace(",", "").trim();

        if (!ValidationUtil.isPositiveDecimal(cleanQty) || !ValidationUtil.isPositiveDecimal(cleanPrice)) {
            validationStatusLabel.setText("Quantity and Price must be positive numbers.");
            return;
        }

        // Structured Address Fields Validation
        String houseNo = houseNoField.getText();
        String street = streetField.getText();
        String area = areaField.getText();
        String city = cityField.getText();
        String district = districtField.getText();
        String state = stateField.getText();
        String pinCode = pinCodeField.getText();
        String country = countryField.getText();

        String addressError = ValidationUtil.validateStructuredAddress(
            houseNo, street, area, city, district, state, pinCode, country
        );

        if (addressError != null) {
            validationStatusLabel.setText(addressError);
            return;
        }

        ValidationUtil.StructuredAddress structuredAddr = new ValidationUtil.StructuredAddress(
            houseNo, street, area, city, district, state, pinCode, country
        );
        String canonicalAddress = structuredAddr.toCanonicalAddress();

        try {
            WasteListing listing = new WasteListing();
            listing.setFarmerId(currentUser.getId());
            listing.setTitle(title);
            listing.setWasteType(wasteType);
            listing.setCategory(categoryCombo.getValue() != null ? categoryCombo.getValue() : WasteCategory.CROP_RESIDUE);
            listing.setDescription(descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");
            listing.setQuantityTons(new BigDecimal(cleanQty));
            listing.setPricePerTon(new BigDecimal(cleanPrice));

            String moist = moistureField.getText();
            if (moist != null && !moist.isBlank()) {
                String cleanMoist = moist.replace("%", "").replace(",", "").trim();
                if (ValidationUtil.isNonNegativeDecimal(cleanMoist)) {
                    listing.setMoistureContentPct(new BigDecimal(cleanMoist));
                }
            }

            listing.setLocation(canonicalAddress);
            listing.setAvailableFrom(availableFromPicker.getValue() != null ? availableFromPicker.getValue() : LocalDate.now());
            listing.setAvailableUntil(availableUntilPicker.getValue());
            listing.setStatus(ListingStatus.AVAILABLE);

            listingRepo.save(listing);

            NotificationService.getInstance().pushNotification(
                currentUser.getId(),
                "LISTING_PUBLISHED",
                "Listing Published",
                "Your listing '" + title + "' (" + cleanQty + " Tons) is now live on the marketplace.",
                null
            );

            // Compact centered success modal
            CompactModal.showSuccess(
                MainLayoutController.getInstance().getRootStackPane(),
                "Listing Published",
                "Your agricultural waste listing '" + title + "' (" + cleanQty + " Tons) has been successfully published to the marketplace.",
                () -> {
                    handleReset();
                    NavigationService.getInstance().navigateTo(ViewType.MY_LISTINGS);
                }
            );

        } catch (Exception e) {
            logger.error("[SELL WASTE ERROR] Failed to publish waste listing: {}", e.getMessage(), e);
            if (e.getCause() instanceof SQLException sqlEx) {
                logger.error("[SELL WASTE SQL DIAGNOSTICS] SQLState: {} | ErrorCode: {} | Message: {}", 
                    sqlEx.getSQLState(), sqlEx.getErrorCode(), sqlEx.getMessage());
            }
            validationStatusLabel.setText("Unable to save listing: " + (e.getMessage() != null ? e.getMessage() : "Database error"));
        }
    }

    private void handleReset() {
        titleField.clear();
        wasteTypeField.clear();
        quantityField.clear();
        priceField.clear();
        moistureField.clear();
        houseNoField.clear();
        streetField.clear();
        areaField.clear();
        cityField.clear();
        districtField.clear();
        stateField.clear();
        pinCodeField.clear();
        countryField.setText("India");
        descriptionArea.clear();
        validationStatusLabel.setText("");
    }
}
