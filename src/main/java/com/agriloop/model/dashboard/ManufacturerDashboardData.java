package com.agriloop.model.dashboard;

import com.agriloop.model.Order;
import com.agriloop.model.WasteListing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates live database metrics and recent activity for the Manufacturer / Buyer Dashboard.
 */
public class ManufacturerDashboardData {
    private BigDecimal materialsPurchasedTons = BigDecimal.ZERO;
    private long activeOrdersCount = 0;
    private long pendingDeliveriesCount = 0;
    private BigDecimal totalSpent = BigDecimal.ZERO;
    private BigDecimal co2ImpactKg = BigDecimal.ZERO;
    private List<Order> recentOrders = new ArrayList<>();
    private List<WasteListing> recommendedMaterials = new ArrayList<>();

    public BigDecimal getMaterialsPurchasedTons() {
        return materialsPurchasedTons;
    }

    public void setMaterialsPurchasedTons(BigDecimal materialsPurchasedTons) {
        this.materialsPurchasedTons = materialsPurchasedTons != null ? materialsPurchasedTons : BigDecimal.ZERO;
    }

    public long getActiveOrdersCount() {
        return activeOrdersCount;
    }

    public void setActiveOrdersCount(long activeOrdersCount) {
        this.activeOrdersCount = activeOrdersCount;
    }

    public long getPendingDeliveriesCount() {
        return pendingDeliveriesCount;
    }

    public void setPendingDeliveriesCount(long pendingDeliveriesCount) {
        this.pendingDeliveriesCount = pendingDeliveriesCount;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;
    }

    public BigDecimal getCo2ImpactKg() {
        return co2ImpactKg;
    }

    public void setCo2ImpactKg(BigDecimal co2ImpactKg) {
        this.co2ImpactKg = co2ImpactKg != null ? co2ImpactKg : BigDecimal.ZERO;
    }

    public List<Order> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<Order> recentOrders) {
        this.recentOrders = recentOrders != null ? recentOrders : new ArrayList<>();
    }

    public List<WasteListing> getRecommendedMaterials() {
        return recommendedMaterials;
    }

    public void setRecommendedMaterials(List<WasteListing> recommendedMaterials) {
        this.recommendedMaterials = recommendedMaterials != null ? recommendedMaterials : new ArrayList<>();
    }
}
