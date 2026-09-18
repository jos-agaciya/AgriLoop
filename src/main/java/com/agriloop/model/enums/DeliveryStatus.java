package com.agriloop.model.enums;

/**
 * Logistics lifecycle states for waste shipments.
 */
public enum DeliveryStatus {
    ASSIGNMENT_PENDING("Assignment Pending"),
    ACCEPTED("Accepted by Transporter"),
    PICKED_UP("Picked Up from Farm"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered to Facility"),
    FAILED("Delivery Failed");

    private final String label;

    DeliveryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
