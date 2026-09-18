package com.agriloop.controller;

import com.agriloop.model.FarmerProfile;
import com.agriloop.model.ManufacturerProfile;
import com.agriloop.model.TransporterProfile;
import com.agriloop.model.User;
import com.agriloop.repository.UserRepository;
import com.agriloop.repository.impl.JdbcUserRepository;
import com.agriloop.service.ProfileService;
import com.agriloop.util.IconHelper;
import com.agriloop.view.components.CompactModal;
import com.agriloop.view.components.ToastNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the customer-facing Stakeholder Profile management page.
 */
public class ProfileViewController implements Initializable {

    @FXML private StackPane profileAvatarIconBox;
    @FXML private Label profileHeaderNameLabel;
    @FXML private Label profileHeaderRoleLabel;
    @FXML private Button changePhotoBtn;
    @FXML private StackPane cameraIconBox;

    // Personal Information
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField locationField;

    // Account Information
    @FXML private Label roleValueLabel;
    @FXML private Label statusValueLabel;
    @FXML private TextArea bioArea;

    // Action Controls
    @FXML private Button editProfileBtn;
    @FXML private Button saveChangesBtn;
    @FXML private Label profileFeedbackLabel;

    private final UserRepository userRepo = new JdbcUserRepository();
    private boolean isEditing = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FontIcon camIcon = IconHelper.createIcon(Feather.CAMERA, 14);
        camIcon.setStyle("-fx-icon-color: #475569;");
        cameraIconBox.getChildren().add(camIcon);

        loadUserProfile();

        editProfileBtn.setOnAction(e -> toggleEditMode(true));
        saveChangesBtn.setOnAction(e -> handleSaveChanges());
        changePhotoBtn.setOnAction(e -> handlePhotoUpload());

        toggleEditMode(false);
    }

    private void loadUserProfile() {
        User current = ProfileService.getInstance().getCurrentUser();
        if (current != null) {
            profileHeaderNameLabel.setText(current.getFullName());
            profileHeaderRoleLabel.setText(current.getRole().getDisplayName());

            fullNameField.setText(current.getFullName());
            emailField.setText(current.getEmail());
            phoneField.setText(current.getPhone() != null ? current.getPhone() : "");
            roleValueLabel.setText(current.getRole().getDisplayName());
            statusValueLabel.setText("Active Member");
            bioArea.setText(current.getBio() != null ? current.getBio() : "");

            updateAvatarDisplay(current.getAvatarUrl());

            // Fetch location from respective role profile if available
            loadRoleLocation(current);
        } else {
            updateAvatarDisplay(null);
        }
    }

    private void updateAvatarDisplay(String avatarUrl) {
        profileAvatarIconBox.getChildren().clear();
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            try {
                Image img = new Image(avatarUrl, 56, 56, true, true, true);
                if (!img.isError()) {
                    ImageView imgView = new ImageView(img);
                    imgView.setFitWidth(48);
                    imgView.setFitHeight(48);
                    Circle clip = new Circle(24, 24, 24);
                    imgView.setClip(clip);
                    profileAvatarIconBox.getChildren().add(imgView);
                    return;
                }
            } catch (Exception ignored) {}
        }
        FontIcon avatarIcon = IconHelper.createIcon(Feather.USER, 32);
        avatarIcon.setStyle("-fx-icon-color: #059669;");
        profileAvatarIconBox.getChildren().add(avatarIcon);
    }

    private void handlePhotoUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(profileAvatarIconBox.getScene().getWindow());
        if (selectedFile != null && selectedFile.exists()) {
            try {
                User current = ProfileService.getInstance().getCurrentUser();
                if (current != null) {
                    String uri = selectedFile.toURI().toString();
                    current.setAvatarUrl(uri);
                    userRepo.save(current);
                    ProfileService.getInstance().setCurrentUser(current);
                    updateAvatarDisplay(uri);
                    ToastNotification.show(MainLayoutController.getInstance().getRootStackPane(), "Profile photo updated successfully.", ToastNotification.Type.SUCCESS);
                }
            } catch (Exception ex) {
                ToastNotification.show(MainLayoutController.getInstance().getRootStackPane(), "Failed to update profile photo.", ToastNotification.Type.ERROR);
            }
        }
    }

    private void loadRoleLocation(User user) {
        try {
            switch (user.getRole()) {
                case FARMER -> {
                    Optional<FarmerProfile> fp = userRepo.findFarmerProfile(user.getId());
                    fp.ifPresent(p -> locationField.setText(p.getFarmLocation()));
                }
                case MANUFACTURER -> {
                    Optional<ManufacturerProfile> mp = userRepo.findManufacturerProfile(user.getId());
                    mp.ifPresent(p -> locationField.setText(p.getFacilityAddress()));
                }
                case TRANSPORTER -> {
                    locationField.setText("Regional Logistics");
                }
                case ADMIN -> locationField.setText("Headquarters");
            }
        } catch (Exception ignored) {}
    }

    private void toggleEditMode(boolean editing) {
        this.isEditing = editing;
        fullNameField.setEditable(editing);
        phoneField.setEditable(editing);
        locationField.setEditable(editing);
        bioArea.setEditable(editing);

        editProfileBtn.setVisible(!editing);
        editProfileBtn.setManaged(!editing);
        saveChangesBtn.setVisible(editing);
        saveChangesBtn.setManaged(editing);

        if (editing) {
            fullNameField.requestFocus();
        }
    }

    private void handleSaveChanges() {
        profileFeedbackLabel.setText("");
        String name = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String location = locationField.getText() != null ? locationField.getText().trim() : "";
        String bio = bioArea.getText() != null ? bioArea.getText().trim() : "";

        if (name.isBlank()) {
            profileFeedbackLabel.setText("Full Name cannot be empty.");
            return;
        }

        User current = ProfileService.getInstance().getCurrentUser();
        if (current != null) {
            try {
                current.setFullName(name);
                current.setPhone(phone);
                current.setBio(bio);
                userRepo.save(current);

                // Update role profile location if applicable
                updateRoleLocation(current, location);

                ProfileService.getInstance().setCurrentUser(current);
                profileHeaderNameLabel.setText(name);

                toggleEditMode(false);
                CompactModal.showSuccess(MainLayoutController.getInstance().getRootStackPane(), "Profile Updated", "Your account profile details were saved successfully.", null);
            } catch (Exception e) {
                profileFeedbackLabel.setText("Failed to save changes. Please try again.");
            }
        }
    }

    private void updateRoleLocation(User user, String location) {
        try {
            switch (user.getRole()) {
                case FARMER -> {
                    Optional<FarmerProfile> fp = userRepo.findFarmerProfile(user.getId());
                    if (fp.isPresent()) {
                        FarmerProfile p = fp.get();
                        p.setFarmLocation(location);
                        userRepo.saveFarmerProfile(p);
                    }
                }
                case MANUFACTURER -> {
                    Optional<ManufacturerProfile> mp = userRepo.findManufacturerProfile(user.getId());
                    if (mp.isPresent()) {
                        ManufacturerProfile p = mp.get();
                        p.setFacilityAddress(location);
                        userRepo.saveManufacturerProfile(p);
                    }
                }
                default -> {}
            }
        } catch (Exception ignored) {}
    }
}
