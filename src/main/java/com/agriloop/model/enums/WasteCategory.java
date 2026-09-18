package com.agriloop.model.enums;

/**
 * Categorization of raw agricultural waste and byproducts.
 */
public enum WasteCategory {
    CROP_RESIDUE("Crop Residue"),
    STALK_AND_STRAW("Stalk & Straw (Wheat, Rice, Corn)"),
    HUSKS_AND_SHELLS("Husks, Pods & Shells"),
    BAGASSE("Sugarcane Bagasse"),
    MANURE_AND_ORGANIC("Animal Manure & Organic Compost"),
    OTHER("Other Biomass / Byproduct");

    private final String displayName;

    WasteCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
