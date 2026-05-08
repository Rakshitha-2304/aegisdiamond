package com.aegisdiamond.shipping.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentEntityTest {

    @Test
    void testShipmentEntityGettersAndSetters() {
        Shipment shipment = new Shipment();

        shipment.setId(1L);
        shipment.setOrigin("New York");
        shipment.setDestination("London");
        shipment.setDiamondIds(List.of(1L, 2L));
        shipment.setShipperId(100L);
        shipment.setStatus("CREATED");
        shipment.setContainerId("CONT-123");
        shipment.setSealId("SEAL-456");
        shipment.setSealed(true);

        assertEquals(1L, shipment.getId());
        assertEquals("New York", shipment.getOrigin());
        assertEquals("London", shipment.getDestination());
        assertEquals(2, shipment.getDiamondIds().size());
        assertEquals(100L, shipment.getShipperId());
        assertEquals("CREATED", shipment.getStatus());
        assertEquals("CONT-123", shipment.getContainerId());
        assertEquals("SEAL-456", shipment.getSealId());
        assertTrue(shipment.isSealed());
    }

    @Test
    void testDefaultStatus() {
        Shipment shipment = new Shipment();
        assertEquals("CREATED", shipment.getStatus());
    }
}
