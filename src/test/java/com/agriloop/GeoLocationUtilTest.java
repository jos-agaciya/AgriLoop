package com.agriloop;

import com.agriloop.util.GeoLocationUtil;
import com.agriloop.util.ValidationUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class GeoLocationUtilTest {

    @Test
    void testExactSameAddressReturnsZeroDistanceAndCost() {
        String addr1 = "12, Gandhi Street, Kundrathur, Chennai, Kanchipuram, Tamil Nadu - 600069, India";
        String addr2 = "12, Gandhi Street, Kundrathur, Chennai, Kanchipuram, Tamil Nadu - 600069, India";

        GeoLocationUtil.RouteCalculation route = GeoLocationUtil.calculateRoute(addr1, addr2, new BigDecimal("10.00"));
        assertNotNull(route);
        assertEquals(0, route.distanceKm().compareTo(BigDecimal.ZERO), "Identical pickup and drop addresses must return 0.00 km");
        assertEquals(0, route.deliveryCost().compareTo(BigDecimal.ZERO), "0.00 km distance must result in ₹0.00 delivery cost");
        assertEquals("Exact (Same Location)", route.distanceTypeLabel());
    }

    @Test
    void testKundrathurToKolathurAccurateDistance() {
        String pickup = "12, Gandhi Street, Kundrathur, Chennai, Kanchipuram, Tamil Nadu - 600069, India";
        String destination = "45, Anna Nagar Main Road, Kolathur, Chennai, Tamil Nadu - 600099, India";

        GeoLocationUtil.RouteCalculation route = GeoLocationUtil.calculateRoute(pickup, destination, new BigDecimal("10.00"));
        assertNotNull(route);
        assertTrue(route.isResolvable(), "Route between Kundrathur and Kolathur must be resolvable");
        assertNotNull(route.distanceKm());

        // Geodesic ~18.8 km, road ~23.5 - 26.5 km. Must NOT be 8.5 km!
        assertTrue(route.distanceKm().compareTo(new BigDecimal("18.00")) > 0, 
            "Kundrathur to Kolathur distance must exceed 18 km, was: " + route.distanceKm());
        assertTrue(route.distanceKm().compareTo(new BigDecimal("35.00")) < 0, 
            "Kundrathur to Kolathur distance should be under 35 km, was: " + route.distanceKm());
        assertNotEquals(new BigDecimal("8.50"), route.distanceKm(), "Must NOT use old 8.5 km fallback");

        assertTrue(route.deliveryCost().compareTo(BigDecimal.ZERO) > 0, "Delivery cost must be computed");
    }

    @Test
    void testStructuredAddressMandatoryFieldsValidation() {
        // Valid complete structured address
        String errValid = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "Kundrathur", "Chennai", "Kanchipuram", "Tamil Nadu", "600069", "India"
        );
        assertNull(errValid, "Valid structured address must have no errors");

        // Missing House Number
        String errHouse = ValidationUtil.validateStructuredAddress(
            "", "Gandhi Street", "Kundrathur", "Chennai", "Kanchipuram", "Tamil Nadu", "600069", "India"
        );
        assertEquals("House / Building Number is required.", errHouse);

        // Missing Street
        String errStreet = ValidationUtil.validateStructuredAddress(
            "12", "   ", "Kundrathur", "Chennai", "Kanchipuram", "Tamil Nadu", "600069", "India"
        );
        assertEquals("Street Name is required.", errStreet);

        // Missing Area
        String errArea = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "", "Chennai", "Kanchipuram", "Tamil Nadu", "600069", "India"
        );
        assertEquals("Area / Locality is required.", errArea);

        // Missing City
        String errCity = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "Kundrathur", "", "Kanchipuram", "Tamil Nadu", "600069", "India"
        );
        assertEquals("City is required.", errCity);

        // Missing District
        String errDist = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "Kundrathur", "Chennai", "", "Tamil Nadu", "600069", "India"
        );
        assertEquals("District is required.", errDist);

        // Missing State
        String errState = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "Kundrathur", "Chennai", "Kanchipuram", "", "600069", "India"
        );
        assertEquals("State is required.", errState);

        // Missing PIN
        String errPin = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "Kundrathur", "Chennai", "Kanchipuram", "Tamil Nadu", "", "India"
        );
        assertEquals("PIN Code is required.", errPin);

        // Invalid PIN format
        String errInvalidPin = ValidationUtil.validateStructuredAddress(
            "12", "Gandhi Street", "Kundrathur", "Chennai", "Kanchipuram", "Tamil Nadu", "6000", "India"
        );
        assertEquals("PIN Code must be a valid 6-digit postal code (e.g. 600069).", errInvalidPin);
    }

    @Test
    void testRealGeographicCoordinateDistanceCalculation() {
        GeoLocationUtil.Coordinates coordPatiala = GeoLocationUtil.resolveCoordinates("Patiala, Punjab");
        GeoLocationUtil.Coordinates coordPanipat = GeoLocationUtil.resolveCoordinates("Industrial Estate Phase 2, Panipat, Haryana");

        assertNotNull(coordPatiala, "Patiala coordinate must be resolvable");
        assertNotNull(coordPanipat, "Panipat coordinate must be resolvable");

        BigDecimal dist = GeoLocationUtil.calculateDistanceKm(
            "Patiala, Punjab", "Industrial Estate Phase 2, Panipat, Haryana"
        );
        // Patiala to Panipat is approx 140-160 km with road tortuosity factor
        assertTrue(dist.compareTo(new BigDecimal("120.00")) > 0 && dist.compareTo(new BigDecimal("180.00")) < 0,
            "Calculated distance should be ~149 km, got: " + dist);

        BigDecimal cost = GeoLocationUtil.calculateDeliveryCost(dist, new BigDecimal("15.00"));
        assertTrue(cost.compareTo(BigDecimal.ZERO) > 0, "Delivery cost must be calculated properly");
    }
}
