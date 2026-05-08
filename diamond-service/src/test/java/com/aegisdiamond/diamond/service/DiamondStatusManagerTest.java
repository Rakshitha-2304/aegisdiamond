package com.aegisdiamond.diamond.service;

import com.aegisdiamond.diamond.dto.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiamondStatusManagerTest {

    private final DiamondStatusManager manager = new DiamondStatusManager();

    @Test
    void getStatusMessage_Registered() {
        DiamondState state = new Registered();
        String message = manager.getStatusMessage(state);
        assertTrue(message.contains("registered"));
    }

    @Test
    void getStatusMessage_Certified() {
        DiamondState state = new Certified("1001");
        String message = manager.getStatusMessage(state);
        assertTrue(message.contains("1001"));
    }

    @Test
    void getStatusMessage_InTransit() {
        DiamondState state = new InTransit(500L);
        String message = manager.getStatusMessage(state);
        assertTrue(message.contains("500"));
    }

    @Test
    void getStatusMessage_InVault() {
        DiamondState state = new InVault(200L);
        String message = manager.getStatusMessage(state);
        assertTrue(message.contains("200"));
    }

    @Test
    void getStatusMessage_Sold() {
        DiamondState state = new Sold("50");
        String message = manager.getStatusMessage(state);
        assertTrue(message.contains("50"));
    }
}
