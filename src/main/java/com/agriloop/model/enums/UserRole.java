package com.agriloop.model.enums;

/**
 * Stakeholder roles within the AgriLoop marketplace ecosystem.
 */
public enum UserRole {
    FARMER("Farmer / Seller", "List agricultural waste, manage inventory, and track sales."),
    MANUFACTURER("Manufacturer / Buyer", "Discover sustainable waste materials, request quotes, and place orders."),
    TRANSPORTER("Transporter / Logistics", "Coordinate waste pickup and delivery logistics with route optimization."),
    ADMIN("Platform Administrator", "System oversight, dispute resolution, and platform health.");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
