package com.agriloop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic & Real-World Geographic Location, Geocoding, and Routing Engine.
 * Supports online live road routing via OSRM / OpenStreetMap with automated fallback
 * to high-precision local coordinate maps and road tortuosity calculations.
 */
public class GeoLocationUtil {
    private static final Logger logger = LoggerFactory.getLogger(GeoLocationUtil.class);

    public record Coordinates(double latitude, double longitude) {}

    public record RouteCalculation(
        BigDecimal distanceKm,
        BigDecimal deliveryCost,
        String distanceTypeLabel, // "Road distance", "Estimated distance", "Exact (Same Location)", "Distance unavailable"
        boolean isResolvable
    ) {}

    // Categorized coordinate dictionaries for hierarchical precision matching
    private static final Map<String, Coordinates> LOCALITY_COORDINATES = new HashMap<>();
    private static final Map<String, Coordinates> CITY_COORDINATES = new HashMap<>();
    private static final Map<String, Coordinates> DISTRICT_COORDINATES = new HashMap<>();

    static {
        // --- Tier 1: Granular Metropolitan Localities & Suburbs ---
        LOCALITY_COORDINATES.put("kundrathur", new Coordinates(12.9977, 80.0972));
        LOCALITY_COORDINATES.put("kolathur", new Coordinates(13.1239, 80.2090));
        LOCALITY_COORDINATES.put("guindy", new Coordinates(13.0067, 80.2026));
        LOCALITY_COORDINATES.put("anna nagar", new Coordinates(13.0850, 80.2100));
        LOCALITY_COORDINATES.put("t nagar", new Coordinates(13.0418, 80.2341));
        LOCALITY_COORDINATES.put("t. nagar", new Coordinates(13.0418, 80.2341));
        LOCALITY_COORDINATES.put("thyagaraya nagar", new Coordinates(13.0418, 80.2341));
        LOCALITY_COORDINATES.put("tambaram", new Coordinates(12.9249, 80.1000));
        LOCALITY_COORDINATES.put("velachery", new Coordinates(12.9759, 80.2212));
        LOCALITY_COORDINATES.put("ambattur", new Coordinates(13.1143, 80.1548));
        LOCALITY_COORDINATES.put("avadi", new Coordinates(13.1147, 80.0982));
        LOCALITY_COORDINATES.put("madhavaram", new Coordinates(13.1488, 80.2314));
        LOCALITY_COORDINATES.put("porur", new Coordinates(13.0382, 80.1565));
        LOCALITY_COORDINATES.put("poonamallee", new Coordinates(13.0489, 80.0886));
        LOCALITY_COORDINATES.put("sholinganallur", new Coordinates(12.9010, 80.2279));
        LOCALITY_COORDINATES.put("perungudi", new Coordinates(12.9654, 80.2461));
        LOCALITY_COORDINATES.put("saidapet", new Coordinates(13.0184, 80.2219));
        LOCALITY_COORDINATES.put("adyar", new Coordinates(13.0012, 80.2565));
        LOCALITY_COORDINATES.put("mylapore", new Coordinates(13.0368, 80.2676));
        LOCALITY_COORDINATES.put("egmore", new Coordinates(13.0827, 80.2612));
        LOCALITY_COORDINATES.put("royapettah", new Coordinates(13.0544, 80.2608));
        LOCALITY_COORDINATES.put("alandur", new Coordinates(12.9975, 80.2006));
        LOCALITY_COORDINATES.put("pallavaram", new Coordinates(12.9675, 80.1491));
        LOCALITY_COORDINATES.put("chromepet", new Coordinates(12.9516, 80.1462));
        LOCALITY_COORDINATES.put("medavakkam", new Coordinates(12.9185, 80.1901));
        LOCALITY_COORDINATES.put("red hills", new Coordinates(13.1991, 80.1967));
        LOCALITY_COORDINATES.put("sriperumbudur", new Coordinates(12.9675, 79.9436));
        LOCALITY_COORDINATES.put("manali", new Coordinates(13.1667, 80.2667));
        LOCALITY_COORDINATES.put("ennore", new Coordinates(13.2136, 80.3236));

        // --- Tier 2: Cities & Agricultural / Industrial Hubs ---
        CITY_COORDINATES.put("chennai", new Coordinates(13.0827, 80.2707));
        CITY_COORDINATES.put("coimbatore", new Coordinates(11.0168, 76.9558));
        CITY_COORDINATES.put("madurai", new Coordinates(9.9252, 78.1198));
        CITY_COORDINATES.put("tiruchirappalli", new Coordinates(10.7905, 78.7047));
        CITY_COORDINATES.put("trichy", new Coordinates(10.7905, 78.7047));
        CITY_COORDINATES.put("salem", new Coordinates(11.6643, 78.1460));
        CITY_COORDINATES.put("tiruppur", new Coordinates(11.1085, 77.3411));
        CITY_COORDINATES.put("tirupur", new Coordinates(11.1085, 77.3411));
        CITY_COORDINATES.put("erode", new Coordinates(11.3410, 77.7172));
        CITY_COORDINATES.put("vellore", new Coordinates(12.9165, 79.1325));
        CITY_COORDINATES.put("thanjavur", new Coordinates(10.7870, 79.1378));
        CITY_COORDINATES.put("dindigul", new Coordinates(10.3673, 77.9803));
        CITY_COORDINATES.put("namakkal", new Coordinates(11.2189, 78.1674));
        CITY_COORDINATES.put("karur", new Coordinates(10.9601, 78.0766));
        CITY_COORDINATES.put("tirunelveli", new Coordinates(8.7139, 77.7567));
        CITY_COORDINATES.put("cuddalore", new Coordinates(11.7480, 79.7714));
        CITY_COORDINATES.put("villupuram", new Coordinates(11.9401, 79.4861));

        CITY_COORDINATES.put("ludhiana", new Coordinates(30.9010, 75.8573));
        CITY_COORDINATES.put("patiala", new Coordinates(30.3398, 76.3869));
        CITY_COORDINATES.put("amritsar", new Coordinates(31.6340, 74.8723));
        CITY_COORDINATES.put("jalandhar", new Coordinates(31.3260, 75.5762));
        CITY_COORDINATES.put("bathinda", new Coordinates(30.2110, 74.9455));
        CITY_COORDINATES.put("panipat", new Coordinates(29.3909, 76.9635));
        CITY_COORDINATES.put("karnal", new Coordinates(29.6857, 76.9905));
        CITY_COORDINATES.put("sonipat", new Coordinates(28.9931, 77.0151));
        CITY_COORDINATES.put("rohtak", new Coordinates(28.8955, 76.6066));
        CITY_COORDINATES.put("ambala", new Coordinates(30.3782, 76.7767));
        CITY_COORDINATES.put("kurukshetra", new Coordinates(29.9695, 76.8783));
        CITY_COORDINATES.put("chandigarh", new Coordinates(30.7333, 76.7794));
        CITY_COORDINATES.put("delhi", new Coordinates(28.6139, 77.2090));
        CITY_COORDINATES.put("new delhi", new Coordinates(28.6139, 77.2090));
        CITY_COORDINATES.put("gurugram", new Coordinates(28.4595, 77.0266));
        CITY_COORDINATES.put("gurgaon", new Coordinates(28.4595, 77.0266));
        CITY_COORDINATES.put("noida", new Coordinates(28.5355, 77.3910));
        CITY_COORDINATES.put("faridabad", new Coordinates(28.4089, 77.3178));

        CITY_COORDINATES.put("bengaluru", new Coordinates(12.9716, 77.5946));
        CITY_COORDINATES.put("bangalore", new Coordinates(12.9716, 77.5946));
        CITY_COORDINATES.put("mysuru", new Coordinates(12.2958, 76.6394));
        CITY_COORDINATES.put("mysore", new Coordinates(12.2958, 76.6394));
        CITY_COORDINATES.put("hyderabad", new Coordinates(17.3850, 78.4867));
        CITY_COORDINATES.put("mumbai", new Coordinates(19.0760, 72.8777));
        CITY_COORDINATES.put("pune", new Coordinates(18.5204, 73.8567));
        CITY_COORDINATES.put("nagpur", new Coordinates(21.1458, 79.0882));
        CITY_COORDINATES.put("nashik", new Coordinates(19.9975, 73.7898));
        CITY_COORDINATES.put("ahmedabad", new Coordinates(23.0225, 72.5714));
        CITY_COORDINATES.put("surat", new Coordinates(21.1702, 72.8311));
        CITY_COORDINATES.put("vadodara", new Coordinates(22.3072, 73.1812));
        CITY_COORDINATES.put("rajkot", new Coordinates(22.3039, 70.8022));
        CITY_COORDINATES.put("jaipur", new Coordinates(26.9124, 75.7873));
        CITY_COORDINATES.put("lucknow", new Coordinates(26.8467, 80.9462));
        CITY_COORDINATES.put("kanpur", new Coordinates(26.4499, 80.3319));
        CITY_COORDINATES.put("varanasi", new Coordinates(25.3176, 82.9739));
        CITY_COORDINATES.put("indore", new Coordinates(22.7196, 75.8577));
        CITY_COORDINATES.put("bhopal", new Coordinates(23.2599, 77.4126));
        CITY_COORDINATES.put("kolkata", new Coordinates(22.5726, 88.3639));
        CITY_COORDINATES.put("patna", new Coordinates(25.5941, 85.1376));

        // --- Tier 3: Wider Districts ---
        DISTRICT_COORDINATES.put("kanchipuram", new Coordinates(12.8342, 79.7036));
        DISTRICT_COORDINATES.put("chengalpattu", new Coordinates(12.6841, 79.9836));
        DISTRICT_COORDINATES.put("tiruvallur", new Coordinates(13.1438, 79.9083));
    }

    /**
     * Calculates the complete route calculation with accurate road or estimated distance.
     */
    public static RouteCalculation calculateRoute(String pickupAddress, String deliveryAddress, BigDecimal cargoTons) {
        if (pickupAddress == null || deliveryAddress == null || pickupAddress.isBlank() || deliveryAddress.isBlank()) {
            return new RouteCalculation(null, null, "Distance unavailable", false);
        }

        String pNorm = normalizeAddress(pickupAddress);
        String dNorm = normalizeAddress(deliveryAddress);

        // 1. Exact address match -> 0.00 km, ₹0.00
        if (pNorm.equals(dNorm) || isExactSameLocation(pNorm, dNorm)) {
            return new RouteCalculation(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "Exact (Same Location)",
                true
            );
        }

        // 2. Try Online Road Routing via Nominatim + OSRM
        try {
            Coordinates pCoordOnline = geocodeOnline(pickupAddress);
            Coordinates dCoordOnline = geocodeOnline(deliveryAddress);

            if (pCoordOnline != null && dCoordOnline != null) {
                BigDecimal osrmRoadDist = calculateOsrmRoadDistance(pCoordOnline, dCoordOnline);
                if (osrmRoadDist != null && osrmRoadDist.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal cost = calculateDeliveryCost(osrmRoadDist, cargoTons);
                    return new RouteCalculation(osrmRoadDist, cost, "Road distance", true);
                }
            }
        } catch (Exception e) {
            logger.debug("Online routing unavailable, using deterministic local calculation: {}", e.getMessage());
        }

        // 3. Fallback: High-Precision Local Coordinate Geodesic + Road Tortuosity
        Coordinates pCoord = resolveCoordinates(pickupAddress);
        Coordinates dCoord = resolveCoordinates(deliveryAddress);

        if (pCoord != null && dCoord != null) {
            double straightKm = haversineDistance(pCoord.latitude, pCoord.longitude, dCoord.latitude, dCoord.longitude);
            if (straightKm < 0.05) {
                return new RouteCalculation(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    "Exact (Same Location)",
                    true
                );
            }
            // Road transit distance is ~1.25x straight-line distance due to road geography
            double roadKm = straightKm * 1.25;
            BigDecimal dist = BigDecimal.valueOf(roadKm).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cost = calculateDeliveryCost(dist, cargoTons);
            return new RouteCalculation(dist, cost, "Estimated distance", true);
        }

        // 4. Truly Unresolvable
        return new RouteCalculation(null, null, "Distance unavailable", false);
    }

    /**
     * Backward-compatible simple distance getter.
     */
    public static BigDecimal calculateDistanceKm(String pickupAddress, String deliveryAddress) {
        RouteCalculation route = calculateRoute(pickupAddress, deliveryAddress, BigDecimal.ONE);
        return route.distanceKm != null ? route.distanceKm : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates delivery cost based on distance and cargo tonnage.
     * If distance is 0 km (same location), delivery cost is ₹0.00.
     */
    public static BigDecimal calculateDeliveryCost(BigDecimal distanceKm, BigDecimal cargoTons) {
        if (distanceKm == null || distanceKm.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (cargoTons == null || cargoTons.compareTo(BigDecimal.ZERO) <= 0) {
            cargoTons = BigDecimal.ONE;
        }

        // Rate: ₹35 per ton per 10km (i.e. ₹3.50/ton-km)
        BigDecimal rate = new BigDecimal("35.00");
        BigDecimal cost = rate.multiply(cargoTons).multiply(distanceKm).divide(new BigDecimal("10.0"), 2, RoundingMode.HALF_UP);

        // Minimum haulage charge for active dispatches
        if (cost.compareTo(new BigDecimal("500.00")) < 0) {
            cost = new BigDecimal("500.00");
        }
        return cost;
    }

    public static Coordinates resolveCoordinates(String address) {
        if (address == null || address.isBlank()) return null;
        String lower = address.toLowerCase(Locale.ROOT);

        // 1. Search in Locality tier first (highest precision)
        Coordinates localityMatch = findBestMatch(lower, LOCALITY_COORDINATES);
        if (localityMatch != null) {
            return localityMatch;
        }

        // 2. Search in City tier
        Coordinates cityMatch = findBestMatch(lower, CITY_COORDINATES);
        if (cityMatch != null) {
            return cityMatch;
        }

        // 3. Search in District tier
        return findBestMatch(lower, DISTRICT_COORDINATES);
    }

    private static Coordinates findBestMatch(String lowerText, Map<String, Coordinates> map) {
        Coordinates bestMatch = null;
        int maxLen = 0;
        for (Map.Entry<String, Coordinates> entry : map.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                if (entry.getKey().length() > maxLen) {
                    maxLen = entry.getKey().length();
                    bestMatch = entry.getValue();
                }
            }
        }
        return bestMatch;
    }

    private static String normalizeAddress(String addr) {
        if (addr == null) return "";
        return addr.toLowerCase(Locale.ROOT)
            .replaceAll("[,\\.\\-\\s/]+", " ")
            .trim();
    }

    private static boolean isExactSameLocation(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.equalsIgnoreCase(b);
    }

    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static Coordinates geocodeOnline(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" + encoded + "&limit=1";
            URI uri = URI.create(urlStr);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "AgriLoop-Desktop-App/1.0");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    String json = response.toString();
                    Pattern latPat = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
                    Pattern lonPat = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher latMat = latPat.matcher(json);
                    Matcher lonMat = lonPat.matcher(json);
                    if (latMat.find() && lonMat.find()) {
                        double lat = Double.parseDouble(latMat.group(1));
                        double lon = Double.parseDouble(lonMat.group(1));
                        return new Coordinates(lat, lon);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static BigDecimal calculateOsrmRoadDistance(Coordinates pCoord, Coordinates dCoord) {
        try {
            String urlStr = String.format(Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%%20%.6f,%.6f?overview=false",
                pCoord.longitude, pCoord.latitude, dCoord.longitude, dCoord.latitude
            );
            URI uri = URI.create(urlStr);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "AgriLoop-Desktop-App/1.0");
            conn.setConnectTimeout(2500);
            conn.setReadTimeout(2500);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    String json = response.toString();
                    Pattern distPat = Pattern.compile("\"distance\"\\s*:\\s*([0-9.]+)");
                    Matcher distMat = distPat.matcher(json);
                    if (distMat.find()) {
                        double meters = Double.parseDouble(distMat.group(1));
                        double km = meters / 1000.0;
                        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
