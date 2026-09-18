package com.agriloop.model.enums;

/**
 * Lifecycle states of an agricultural waste listing.
 */
public enum ListingStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    SOLD_OUT("Sold Out"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled");

    private final String label;

    ListingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
