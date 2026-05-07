package com.aegisdiamond.insurance.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValuationServiceTest {

    private final ValuationService valuationService = new ValuationService();

    @Test
    void testCalculateDiamondValue() {
        // Base Price: 1000, Carat: 2, Multiplier: 1.5
        // Expected: 1000 * 2 * 1.5 = 3000
        double value = valuationService.calculateDiamondValue(1000, 2, 1.5);
        assertEquals(3000.0, value, 0.001);
    }

    @Test
    void testCalculateDiamondValueHigh() {
        // Base Price: 50000, Carat: 5.5, Multiplier: 2.0
        // Expected: 50000 * 5.5 * 2 = 550000
        double value = valuationService.calculateDiamondValue(50000, 5.5, 2.0);
        assertEquals(550000.0, value, 0.001);
    }
}
