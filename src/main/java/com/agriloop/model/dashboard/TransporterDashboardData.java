package com.agriloop.model.dashboard;

import com.agriloop.model.Delivery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates live database metrics and recent activity for the Transporter Dashboard.
 */
public class TransporterDashboardData {
    private long pendingRequestsCount = 0;
    private long activeDeliveriesCount = 0;
    private long completedDeliveriesCount = 0;
    private BigDecimal totalEarnings = BigDecimal.ZERO;
    private BigDecimal distanceCoveredKm = BigDecimal.ZERO;
    private List<Delivery> recentDeliveries = new ArrayList<>();

    public long getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(long pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public long getActiveDeliveriesCount() {
        return activeDeliveriesCount;
    }

    public void setActiveDeliveriesCount(long activeDeliveriesCount) {
        this.activeDeliveriesCount = activeDeliveriesCount;
    }

    public long getCompletedDeliveriesCount() {
        return completedDeliveriesCount;
    }

    public void setCompletedDeliveriesCount(long completedDeliveriesCount) {
        this.completedDeliveriesCount = completedDeliveriesCount;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings != null ? totalEarnings : BigDecimal.ZERO;
    }

    public BigDecimal getDistanceCoveredKm() {
        return distanceCoveredKm;
    }

    public void setDistanceCoveredKm(BigDecimal distanceCoveredKm) {
        this.distanceCoveredKm = distanceCoveredKm != null ? distanceCoveredKm : BigDecimal.ZERO;
    }

    public List<Delivery> getRecentDeliveries() {
        return recentDeliveries;
    }

    public void setRecentDeliveries(List<Delivery> recentDeliveries) {
        this.recentDeliveries = recentDeliveries != null ? recentDeliveries : new ArrayList<>();
    }
}
