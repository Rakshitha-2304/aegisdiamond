package com.aegisdiamond.fraud.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiFraudEngineTest {

    private final AiFraudEngine fraudEngine = new AiFraudEngine();

    @Test
    void testCalculateFraudProbability() {
        assertEquals(0.95, fraudEngine.calculateFraudProbability("FRAUD DETECTED: Duplicate certificate ID found."));
        assertEquals(0.65, fraudEngine.calculateFraudProbability("Activity is SUSPICIOUS: Unusually long delay at customs."));
        assertEquals(0.05, fraudEngine.calculateFraudProbability("Everything looks SECURE."));
    }
}
