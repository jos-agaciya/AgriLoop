package com.agriloop.service.intelligence;

import com.agriloop.model.WasteListing;
import com.agriloop.model.enums.WasteCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic rule-based matching engine for agricultural residues and industrial applications.
 */
public class SmartWasteMatchingService {

    public record IndustrialMatch(
        String applicationTitle,
        String targetIndustry,
        String processingMethod,
        int suitabilityScore,
        String keyBenefit
    ) {}

    private static SmartWasteMatchingService instance;

    private SmartWasteMatchingService() {}

    public static synchronized SmartWasteMatchingService getInstance() {
        if (instance == null) {
            instance = new SmartWasteMatchingService();
        }
        return instance;
    }

    /**
     * Evaluates a waste listing and returns suitable industrial applications.
     */
    public List<IndustrialMatch> findMatches(WasteListing listing) {
        List<IndustrialMatch> matches = new ArrayList<>();
        if (listing == null) return matches;

        WasteCategory category = listing.getCategory() != null ? listing.getCategory() : WasteCategory.CROP_RESIDUE;
        String typeLower = listing.getWasteType() != null ? listing.getWasteType().toLowerCase() : "";

        switch (category) {
            case STALK_AND_STRAW -> {
                if (typeLower.contains("wheat") || typeLower.contains("paddy") || typeLower.contains("rice")) {
                    matches.add(new IndustrialMatch(
                        "High-Density Biomass Pellets / Briquettes",
                        "Clean Energy & Thermal Power",
                        "Drying, Pulverizing & Hydraulic Briquetting",
                        95,
                        "Replaces fossil coal in industrial boilers with net-zero carbon emissions"
                    ));
                    matches.add(new IndustrialMatch(
                        "Bio-Degradable Packaging & Pulp Paper",
                        "Packaging & Paper Manufacturing",
                        "Cellulose Pulping & Thermo-molding",
                        88,
                        "Sturdy eco-friendly alternative to single-use Styrofoam and plastic packaging"
                    ));
                    matches.add(new IndustrialMatch(
                        "Mushroom Spawn Cultivation Substrate",
                        "Specialty Agriculture & Mycology",
                        "Pasteurization & Mineral Fortification",
                        82,
                        "Nutrient-dense substrate for high-yield Oyster and Button mushroom cultivation"
                    ));
                } else if (typeLower.contains("cotton") || typeLower.contains("mustard")) {
                    matches.add(new IndustrialMatch(
                        "Particle Board & Bio-Composites",
                        "Furniture & Construction Panels",
                        "Chipping, Resin Blending & Hot Pressing",
                        92,
                        "High-tensile engineered wood panels without deforestation"
                    ));
                    matches.add(new IndustrialMatch(
                        "Industrial Activated Carbon / Biochar",
                        "Water Purification & Soil Amendment",
                        "Oxygen-Deprived Thermal Pyrolysis (550°C)",
                        86,
                        "Captures permanent recalcitrant soil carbon while filtering heavy metals"
                    ));
                } else {
                    matches.add(new IndustrialMatch(
                        "Densified Biomass Fuel",
                        "Thermal Power & Boilers",
                        "Pelletization",
                        90,
                        "Standardized clean energy density for industrial kilns"
                    ));
                    matches.add(new IndustrialMatch(
                        "Lignocellulosic Fiber Board",
                        "Building & Construction",
                        "Compression Molding",
                        84,
                        "Durable insulating building materials"
                    ));
                }
            }
            case HUSKS_AND_SHELLS -> {
                if (typeLower.contains("rice") || typeLower.contains("paddy")) {
                    matches.add(new IndustrialMatch(
                        "Green Precipitated Silica Extraction",
                        "Tire Manufacturing & Pharmaceuticals",
                        "Controlled Alkaline Leaching & Calcination",
                        96,
                        "High-grade micro-silica substituting petroleum-derived chemical additives"
                    ));
                    matches.add(new IndustrialMatch(
                        "Bio-Charcoal Briquettes",
                        "Commercial Smelting & Heating",
                        "Carbonization & Starch-Bound Pressing",
                        91,
                        "Smokeless, high calorific value (4,200 kcal/kg) fuel"
                    ));
                } else if (typeLower.contains("coconut") || typeLower.contains("coir")) {
                    matches.add(new IndustrialMatch(
                        "Activated Carbon Supercapacitor Grade",
                        "Energy Storage & Filtration",
                        "Steam Activation & Acid Washing",
                        98,
                        "Ultra-high specific surface area (1,100+ m²/g) for water and air purifiers"
                    ));
                    matches.add(new IndustrialMatch(
                        "Horticultural Coir Pith Substrate",
                        "Hydroponics & Greenhouse Farming",
                        "Desalination & Moisture Conditioning",
                        94,
                        "Superior water retention capacity (8x dry weight) replacing peat moss"
                    ));
                } else {
                    matches.add(new IndustrialMatch(
                        "Bio-Abrasives & Polishing Media",
                        "Precision Metal Finishing",
                        "Fine Mechanical Milling",
                        88,
                        "Non-toxic surface finishing media for aerospace and automotive parts"
                    ));
                }
            }
            case BAGASSE -> {
                matches.add(new IndustrialMatch(
                    "Molded Pulp Food Tableware",
                    "Sustainable Packaging",
                    "Thermoforming & Water/Oil Repellent Treatment",
                    97,
                    "100% home compostable containers, plates, and delivery trays"
                ));
                matches.add(new IndustrialMatch(
                    "Second-Generation Bio-Ethanol",
                    "Renewable Transport Fuel",
                    "Enzymatic Hydrolysis & Yeast Fermentation",
                    89,
                    "Blended E20 green fuel reducing fossil fuel import reliance"
                ));
                matches.add(new IndustrialMatch(
                    "Unbleached Kraft Paper Pulp",
                    "Paper & Corrugated Packaging",
                    "Soda Pulping Process",
                    85,
                    "High-strength liner board for shipping cartons"
                ));
            }
            case MANURE_AND_ORGANIC -> {
                matches.add(new IndustrialMatch(
                    "Compressed Biomethane (CBG / Bio-CNG)",
                    "Automotive & Clean Utility Gas",
                    "Anaerobic Digestion & Membrane Gas Upgrading",
                    96,
                    "Replaces fossil CNG; yields 95%+ pure methane for transport grid"
                ));
                matches.add(new IndustrialMatch(
                    "Fermented Organic Solid Fertilizer (PROM)",
                    "Organic Farming & Regenerative Soils",
                    "Aerobic Windrow Composting & Phosphate Enrichment",
                    93,
                    "Restores topsoil microbial biodiversity and organic carbon index"
                ));
            }
            case CROP_RESIDUE, OTHER -> {
                matches.add(new IndustrialMatch(
                    "Industrial Anaerobic Biogas",
                    "Grid Power & Thermal Co-generation",
                    "Co-digestion & High-Rate Bioreactor",
                    90,
                    "Continuous decentralized renewable electricity and heat generation"
                ));
                matches.add(new IndustrialMatch(
                    "Soil Regeneration Humus Pellets",
                    "Agri-Input & Land Reclamation",
                    "Microbial Inoculation & Granulation",
                    84,
                    "Enhances soil cation exchange capacity and long-term water conservation"
                ));
            }
        }

        return matches;
    }
}
