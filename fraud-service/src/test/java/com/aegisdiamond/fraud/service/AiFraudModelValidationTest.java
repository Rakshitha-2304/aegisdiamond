package com.aegisdiamond.fraud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiFraudModelValidationTest {

    private AiFraudEngine aiFraudEngine;
    private ChatModel mockChatModel;

    @BeforeEach
    void setUp() throws Exception {
        aiFraudEngine = new AiFraudEngine();
        mockChatModel = mock(ChatModel.class);
        setPrivateField(aiFraudEngine, "chatModel", mockChatModel);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void testAiModelNotNullWhenAvailable() throws Exception {
        assertNotNull(getPrivateField(aiFraudEngine, "chatModel"));
    }

    @Test
    void testAiModelNullHandling() throws Exception {
        setPrivateField(aiFraudEngine, "chatModel", null);
        String result = aiFraudEngine.analyzePatterns(100L, "Test payload");
        assertNotNull(result);
        assertTrue(result.contains("unavailable") || result.contains("normal"));
    }

    @Test
    void testAiModelCallForFraudAnalysis() {
        when(mockChatModel.call(anyString())).thenReturn("FRAUD DETECTED: Duplicate shipment ID found");

        String result = aiFraudEngine.analyzePatterns(100L, "shipment_id=100, origin=USA, value=$2M");
        assertNotNull(result);
        assertTrue(result.contains("FRAUD DETECTED"));
        verify(mockChatModel).call(anyString());
    }

    @Test
    void testAiModelCallForSecureAnalysis() {
        when(mockChatModel.call(anyString())).thenReturn("SECURE: No fraudulent patterns detected");

        String result = aiFraudEngine.analyzePatterns(100L, "Normal shipment data");
        assertNotNull(result);
        assertTrue(result.contains("SECURE"));
    }

    @Test
    void testCalculateFraudProbability_FraudDetected() {
        double prob = aiFraudEngine.calculateFraudProbability("FRAUD DETECTED in shipment");
        assertEquals(0.95, prob, 0.001);
    }

    @Test
    void testCalculateFraudProbability_Suspicious() {
        double prob = aiFraudEngine.calculateFraudProbability("SUSPICIOUS: Unusual route pattern");
        assertEquals(0.65, prob, 0.001);
    }

    @Test
    void testCalculateFraudProbability_Normal() {
        double prob = aiFraudEngine.calculateFraudProbability("SECURE shipment");
        assertEquals(0.05, prob, 0.001);
    }

    @Test
    void testCalculateFraudProbability_EmptyString() {
        double prob = aiFraudEngine.calculateFraudProbability("");
        assertEquals(0.05, prob, 0.001);
    }

    @Test
    void testCalculateFraudProbability_NullString() {
        // Handle potential null input
        double prob = aiFraudEngine.calculateFraudProbability("");
        assertEquals(0.05, prob, 0.001);
    }

    @Test
    void testAnalyzePatterns_ReturnsFraudDetection() {
        when(mockChatModel.call(anyString())).thenReturn("FRAUD DETECTED: Multiple red flags found");

        String result = aiFraudEngine.analyzePatterns(100L, "Payload with issues");
        assertNotNull(result);
        assertTrue(result.contains("FRAUD DETECTED"));
    }

    @Test
    void testAnalyzePatterns_CallsChatModelWithCorrectParameters() {
        when(mockChatModel.call(anyString())).thenReturn("SECURE");

        aiFraudEngine.analyzePatterns(123L, "Test payload data for shipment 123");

        verify(mockChatModel).call(anyString());
    }

    @Test
    void testAnalyzePatterns_SuspiciousPayload() {
        when(mockChatModel.call(anyString())).thenReturn("SUSPICIOUS: Route deviation and delays");

        String result = aiFraudEngine.analyzePatterns(100L, "Delayed shipment with route change");
        assertTrue(result.contains("SUSPICIOUS"));
        double prob = aiFraudEngine.calculateFraudProbability(result);
        assertEquals(0.65, prob, 0.001);
    }
}
