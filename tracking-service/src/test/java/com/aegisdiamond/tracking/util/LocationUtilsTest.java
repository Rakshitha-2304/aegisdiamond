package com.aegisdiamond.tracking.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocationUtilsTest {

    @Test
    void testCalculateDistance() {
        // Coordinates for London and Paris
        double lat1 = 51.5074;
        double lon1 = -0.1278;
        double lat2 = 48.8566;
        double lon2 = 2.3522;

        double distance = LocationUtils.calculateDistance(lat1, lon1, lat2, lon2);
        
        // Expected distance is approximately 343 km
        assertTrue(distance > 340 && distance < 350, "Distance should be around 343 km");
    }

    @Test
    void testCalculateDistanceSamePoint() {
        double lat = 51.5074;
        double lon = -0.1278;
        
        double distance = LocationUtils.calculateDistance(lat, lon, lat, lon);
        assertEquals(0, distance, 0.001);
    }
}
