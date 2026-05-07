package com.aegisdiamond.notification.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlertDispatcherTest {

    private final AlertDispatcher alertDispatcher = new AlertDispatcher();

    @Test
    void testDispatch() {
        // This test mainly ensures no exceptions are thrown during dispatch
        assertDoesNotThrow(() -> alertDispatcher.dispatch("Test Alert", "Test Message", "HIGH"));
    }
}
