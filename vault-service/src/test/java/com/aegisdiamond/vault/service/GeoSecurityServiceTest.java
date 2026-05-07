package com.aegisdiamond.vault.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoSecurityServiceTest {

    private final GeoSecurityService geoSecurityService = new GeoSecurityService();

    @Test
    void testIsLocationSecure() {
        // Vault Location
        double vaultLat = 51.5074;
        double vaultLon = -0.1278;

        // Secure Request (very close)
        assertTrue(geoSecurityService.isLocationSecure(51.5075, -0.1279, vaultLat, vaultLon));

        // Insecure Request (far away)
        assertFalse(geoSecurityService.isLocationSecure(48.8566, 2.3522, vaultLat, vaultLon));
    }
}
