package com.aegisdiamond.diamond.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiamondEntityTest {

    @Test
    void testDiamondEntityGettersAndSetters() {
        Diamond diamond = new Diamond();

        diamond.setId(1L);
        diamond.setCut("Excellent");
        diamond.setClarity("VVS1");
        diamond.setColor("D");
        diamond.setCarat(2.5);
        diamond.setCertificateId(1001L);
        diamond.setOwnerId(10L);
        diamond.setStatus("REGISTERED");

        assertEquals(1L, diamond.getId());
        assertEquals("Excellent", diamond.getCut());
        assertEquals("VVS1", diamond.getClarity());
        assertEquals("D", diamond.getColor());
        assertEquals(2.5, diamond.getCarat(), 0.001);
        assertEquals(1001L, diamond.getCertificateId());
        assertEquals(10L, diamond.getOwnerId());
        assertEquals("REGISTERED", diamond.getStatus());
    }

    @Test
    void testDiamondEntityDefaultConstructor() {
        Diamond diamond = new Diamond();
        assertNull(diamond.getId());
        assertNull(diamond.getCut());
        assertEquals(0.0, diamond.getCarat(), 0.001);
    }
}
