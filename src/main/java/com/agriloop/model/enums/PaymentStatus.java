package com.agriloop.model.enums;

/**
 * Escrow and payment settlement states.
 */
public enum PaymentStatus {
    UNPAID("Unpaid"),
    ESCROW_HELD("Held in Escrow"),
    RELEASED_TO_SELLER("Released to Seller"),
    REFUNDED("Refunded");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
