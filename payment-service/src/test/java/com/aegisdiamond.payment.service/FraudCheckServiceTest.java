package com.aegisdiamond.payment.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FraudCheckServiceTest {

    private final FraudCheckService fraudCheckService = new FraudCheckService();

    @Test
    void testIsTransactionSafe_NormalAmount() {
        assertTrue(fraudCheckService.isTransactionSafe(101L, 50000.0));
    }

    @Test
    void testIsTransactionSafe_HighValue() {
        // Amount over 5,000,000 should fail in our mock
        assertFalse(fraudCheckService.isTransactionSafe(102L, 6000000.0));
    }
}
