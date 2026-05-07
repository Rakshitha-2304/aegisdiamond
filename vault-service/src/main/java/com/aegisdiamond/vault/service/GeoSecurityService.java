package com.aegisdiamond.vault.service;

import org.springframework.stereotype.Service;

@Service
public class GeoSecurityService {
    private static final int EARTH_RADIUS_KM = 6371;
    private static final double MAX_ALLOWED_DISTANCE_KM = 1.0; // 1km tolerance for secure access

    public boolean isLocationSecure(double actualLat, double actualLon, double expectedLat, double expectedLon) {
        double dLat = Math.toRadians(expectedLat - actualLat);
        double dLon = Math.toRadians(expectedLon - actualLon);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(actualLat)) * Math.cos(Math.toRadians(expectedLat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return distance <= MAX_ALLOWED_DISTANCE_KM;
    }
}
