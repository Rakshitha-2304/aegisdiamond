package com.aegisdiamond.shipping.service;

import com.aegisdiamond.shipping.dto.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentStatusManagerTest {

    private final ShipmentStatusManager statusManager = new ShipmentStatusManager();

    @Test
    void testGetStatusMessage() {
        assertEquals("Shipment has been created and is awaiting verification.", statusManager.getStatusMessage(new Created()));
        assertEquals("Shipment has been verified and is ready for sealing.", statusManager.getStatusMessage(new Verified()));
        assertEquals("Shipment is sealed and ready for transit.", statusManager.getStatusMessage(new Sealed()));
        assertEquals("Shipment is currently in transit.", statusManager.getStatusMessage(new InTransit()));
        assertEquals("Shipment is undergoing customs clearance.", statusManager.getStatusMessage(new Customs()));
        assertEquals("Shipment has been delivered successfully.", statusManager.getStatusMessage(new Delivered()));
        assertEquals("Shipment is closed and archived.", statusManager.getStatusMessage(new Closed()));
    }

    @Test
    void testFromString() {
        assertTrue(statusManager.fromString("CREATED") instanceof Created);
        assertTrue(statusManager.fromString("VERIFIED") instanceof Verified);
        assertTrue(statusManager.fromString("SEALED") instanceof Sealed);
        assertTrue(statusManager.fromString("IN_TRANSIT") instanceof InTransit);
        assertTrue(statusManager.fromString("CUSTOMS") instanceof Customs);
        assertTrue(statusManager.fromString("DELIVERED") instanceof Delivered);
        assertTrue(statusManager.fromString("CLOSED") instanceof Closed);
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> statusManager.fromString("INVALID"));
    }
}
