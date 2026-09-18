package com.agriloop.model.enums;

/**
 * Lifecycle states of a waste purchase order.
 */
public enum OrderStatus {
    PENDING("Pending Confirmation"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
