package com.agriloop.controller;

import com.agriloop.database.DatabaseManager;
import com.agriloop.model.SustainabilityRecord;
import com.agriloop.model.User;
import com.agriloop.model.enums.UserRole;
import com.agriloop.repository.SustainabilityRepository;
import com.agriloop.repository.impl.JdbcSustainabilityRepository;
import com.agriloop.service.NavigationService;
import com.agriloop.service.ProfileService;
import com.agriloop.view.ViewType;
import com.agriloop.view.components.EmptyStateCard;
import com.agriloop.view.components.StatCard;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for Sustainability & CO2 Environmental Impact View.
 */
public class ImpactViewController implements Initializable {

    @FXML private HBox impactStatsRow;
    @FXML private VBox impactHistoryContainer;

    private final SustainabilityRepository sustainabilityRepo = new JdbcSustainabilityRepository();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupImpactStats();
        loadImpactRecords();
    }

    private void setupImpactStats() {
        impactStatsRow.getChildren().clear();

        User current = ProfileService.getInstance().getCurrentUser();
        BigDecimal wasteDiverted = BigDecimal.ZERO;
        BigDecimal co2Saved = BigDecimal.ZERO;
        BigDecimal methanePrevented = BigDecimal.ZERO;
        BigDecimal energyGen = BigDecimal.ZERO;

        try {
            if (current != null) {
                wasteDiverted = sustainabilityRepo.getWasteDivertedByUserId(current.getId());
                co2Saved = sustainabilityRepo.getCo2SavedByUserId(current.getId());
                // Methane and energy scaled proportionally to CO2
                methanePrevented = co2Saved.multiply(BigDecimal.valueOf(0.12));
                energyGen = wasteDiverted.multiply(BigDecimal.valueOf(420.0));
            } else {
                wasteDiverted = sustainabilityRepo.getTotalWasteDivertedTons();
                co2Saved = sustainabilityRepo.getTotalCo2SavedKg();
                methanePrevented = sustainabilityRepo.getTotalMethanePreventedKg();
                energyGen = sustainabilityRepo.getTotalEnergyGeneratedKwh();
            }
        } catch (Exception ignored) {}

        String card1Title = "Biomass Diverted";
        String card1Sub = "Prevented from field burning";
        String card2Title = "CO₂ Avoided";
        String card2Sub = "Atmospheric emission savings";

        if (current != null && current.getRole() == UserRole.FARMER) {
            card1Title = "Waste Diverted via Sales";
            card1Sub = "Agricultural residue sold & diverted";
            card2Title = "CO₂ Avoided from Field Burning";
            card2Sub = "Direct environmental contribution";
        } else if (current != null && current.getRole() == UserRole.MANUFACTURER) {
            card1Title = "Sustainable Materials Sourced";
            card1Sub = "Biomass procured for processing";
            card2Title = "Supply Chain CO₂ Avoided";
            card2Sub = "Circular economy displacement";
        }

        StatCard card1 = new StatCard(Feather.ACTIVITY, card1Title, String.format("%.2f Tons", wasteDiverted), card1Sub, StatCard.StatTheme.EMERALD);
        StatCard card2 = new StatCard(Feather.SHIELD, card2Title, String.format("%.2f kg", co2Saved), card2Sub, StatCard.StatTheme.BLUE);
        StatCard card3 = new StatCard(Feather.ALERT_TRIANGLE, "Methane Prevented", String.format("%.2f kg", methanePrevented), "Decomposition avoidance", StatCard.StatTheme.AMBER);
        StatCard card4 = new StatCard(Feather.CPU, "Energy Potential", String.format("%.2f kWh", energyGen), "Bio-energy valorization", StatCard.StatTheme.PURPLE);

        impactStatsRow.getChildren().addAll(card1, card2, card3, card4);
    }

    private void loadImpactRecords() {
        impactHistoryContainer.getChildren().clear();
        User current = ProfileService.getInstance().getCurrentUser();
        List<SustainabilityRecord> list = List.of();

        try {
            if (current != null) {
                list = sustainabilityRepo.findByUserId(current.getId());
            } else {
                list = sustainabilityRepo.findAll();
            }
        } catch (Exception ignored) {}

        if (list.isEmpty()) {
            String btnText = "Explore Features";
            Runnable action = () -> NavigationService.getInstance().navigateTo(ViewType.DASHBOARD);

            if (current != null) {
                if (current.getRole() == UserRole.FARMER) {
                    btnText = "Publish Agricultural Waste";
                    action = () -> NavigationService.getInstance().navigateTo(ViewType.SELL_WASTE);
                } else if (current.getRole() == UserRole.MANUFACTURER) {
                    btnText = "Browse Materials";
                    action = () -> NavigationService.getInstance().navigateTo(ViewType.MARKETPLACE);
                } else if (current.getRole() == UserRole.TRANSPORTER) {
                    btnText = "View Delivery Requests";
                    action = () -> NavigationService.getInstance().navigateTo(ViewType.DELIVERIES);
                }
            }

            EmptyStateCard emptyState = new EmptyStateCard(
                Feather.ACTIVITY,
                "No sustainability calculations recorded yet",
                "Environmental impact records are generated when agricultural waste transactions complete. Diverting crop residues from open field burning directly offsets carbon emissions.",
                btnText,
                action
            );
            impactHistoryContainer.getChildren().add(emptyState);
        } else {
            for (SustainabilityRecord rec : list) {
                HBox card = new HBox(16);
                card.getStyleClass().add("card");
                VBox box = new VBox(4);
                Label title = new Label("Impact Audit • " + (rec.getCalculationDate() != null ? rec.getCalculationDate().toString() : "Recent"));
                title.getStyleClass().add("heading-3");
                Label details = new Label(String.format("Diverted: %.2f Tons • CO2 Saved: %.2f kg • Methane Prevented: %.2f kg • Energy: %.2f kWh", 
                    rec.getWasteDivertedTons(), rec.getCo2SavedKg(), rec.getMethanePreventedKg(), rec.getEnergyGeneratedKwh()));
                details.getStyleClass().add("subheading");
                box.getChildren().addAll(title, details);
                card.getChildren().add(box);
                impactHistoryContainer.getChildren().add(card);
            }
        }
    }
}
