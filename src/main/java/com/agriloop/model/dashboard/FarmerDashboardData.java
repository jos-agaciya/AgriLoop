package com.agriloop.model.dashboard;

import com.agriloop.model.Order;
import com.agriloop.model.WasteListing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates live database metrics and recent activity for the Farmer / Seller Dashboard.
 */
public class FarmerDashboardData {
    private long activeListingsCount = 0;
    private BigDecimal wasteAvailableTons = BigDecimal.ZERO;
    private long pendingRequestsCount = 0;
    private long completedSalesCount = 0;
    private BigDecimal totalEarnings = BigDecimal.ZERO;
    private BigDecimal wasteSoldTons = BigDecimal.ZERO;
    private BigDecimal co2ImpactKg = BigDecimal.ZERO;
    private List<WasteListing> recentListings = new ArrayList<>();
    private List<Order> incomingRequests = new ArrayList<>();

    public long getActiveListingsCount() { return activeListingsCount; }
    public void setActiveListingsCount(long activeListingsCount) { this.activeListingsCount = activeListingsCount; }

    public BigDecimal getWasteAvailableTons() { return wasteAvailableTons; }
    public void setWasteAvailableTons(BigDecimal wasteAvailableTons) { this.wasteAvailableTons = wasteAvailableTons != null ? wasteAvailableTons : BigDecimal.ZERO; }

    public long getPendingRequestsCount() { return pendingRequestsCount; }
    public void setPendingRequestsCount(long pendingRequestsCount) { this.pendingRequestsCount = pendingRequestsCount; }

    public long getCompletedSalesCount() { return completedSalesCount; }
    public void setCompletedSalesCount(long completedSalesCount) { this.completedSalesCount = completedSalesCount; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings != null ? totalEarnings : BigDecimal.ZERO; }

    public BigDecimal getWasteSoldTons() { return wasteSoldTons; }
    public void setWasteSoldTons(BigDecimal wasteSoldTons) { this.wasteSoldTons = wasteSoldTons != null ? wasteSoldTons : BigDecimal.ZERO; }

    public BigDecimal getCo2ImpactKg() { return co2ImpactKg; }
    public void setCo2ImpactKg(BigDecimal co2ImpactKg) { this.co2ImpactKg = co2ImpactKg != null ? co2ImpactKg : BigDecimal.ZERO; }

    public List<WasteListing> getRecentListings() { return recentListings; }
    public void setRecentListings(List<WasteListing> recentListings) { this.recentListings = recentListings != null ? recentListings : new ArrayList<>(); }

    public List<Order> getIncomingRequests() { return incomingRequests; }
    public void setIncomingRequests(List<Order> incomingRequests) { this.incomingRequests = incomingRequests != null ? incomingRequests : new ArrayList<>(); }

    // Backward-compatibility helpers
    public long getPendingOrdersCount() { return pendingRequestsCount; }
    public void setPendingOrdersCount(long count) { this.pendingRequestsCount = count; }
    public List<Order> getIncomingOrders() { return incomingRequests; }
    public void setIncomingOrders(List<Order> incomingOrders) { this.incomingRequests = incomingOrders; }
}
