package com.aegisdiamond.payment.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FraudCheckServiceTest {

    private final FraudCheckService fraudCheckService = new FraudCheckService();

    @Test
    void testIsTransactionSafe_NormalAmount() {
        assertTrue(fraudCheckService.isTransactionSafe("tx-1", 50000.0));
    }

    @Test
    void testIsTransactionSafe_HighValue() {
        // Amount over 5,000,000 should fail in our mock
        assertFalse(fraudCheckService.isTransactionSafe("tx-2", 6000000.0));
    }
}
