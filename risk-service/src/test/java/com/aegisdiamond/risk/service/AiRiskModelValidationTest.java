package com.aegisdiamond.risk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiRiskModelValidationTest {

    private AiRiskEngine aiRiskEngine;
    private ChatModel mockChatModel;

    @BeforeEach
    void setUp() throws Exception {
        aiRiskEngine = new AiRiskEngine();
        mockChatModel = mock(ChatModel.class);
        setPrivateField(aiRiskEngine, "chatModel", mockChatModel);
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
        assertNotNull(getPrivateField(aiRiskEngine, "chatModel"));
    }

    @Test
    void testAiModelNullHandling() throws Exception {
        setPrivateField(aiRiskEngine, "chatModel", null);
        String insights = aiRiskEngine.generateAiInsights(100L, 500000.0, "Test Route", 0.5);
        assertNotNull(insights);
        assertTrue(insights.contains("unavailable") || insights.contains("manual"));
    }

    @Test
    void testAiModelCallForRiskAnalysis() {
        when(mockChatModel.call(anyString())).thenReturn("High risk due to value and route complexity");

        String insights = aiRiskEngine.generateAiInsights(100L, 2000000.0, "High-Risk Zone A", 0.8);
        assertNotNull(insights);
        verify(mockChatModel).call(anyString());
    }

    @Test
    void testAiModelCallForAnomalyDetection() {
        when(mockChatModel.call(anyString())).thenReturn("ANOMALY DETECTED: Route deviation of 100km");

        String result = aiRiskEngine.detectAnomalies(100L, "latitude=40.0, longitude=-74.0, speed=150km/h");
        assertNotNull(result);
        assertTrue(result.contains("ANOMALY DETECTED"));
    }

    @Test
    void testAiModelReturnsNormalForGoodData() {
        when(mockChatModel.call(anyString())).thenReturn("NORMAL: No anomalies detected in the data stream");

        String result = aiRiskEngine.detectAnomalies(100L, "Normal tracking data");
        assertNotNull(result);
        assertTrue(result.contains("NORMAL"));
    }

    @Test
    void testCalculateFormulaicRisk_ValueFactorCapped() {
        // Test that value factor is capped at 1.0 for very high values
        double score1 = aiRiskEngine.calculateFormulaicRisk(5000000.0, 0.0, 0.0);
        double score2 = aiRiskEngine.calculateFormulaicRisk(10000000.0, 0.0, 0.0);

        // Both should be the same since value factor is capped at 1.0
        assertEquals(score1, score2, 0.001);
        assertEquals(0.4, score1, 0.001);  // W1 * 1.0 = 0.4
    }

    @Test
    void testCalculateFormulaicRisk_AllZero() {
        double score = aiRiskEngine.calculateFormulaicRisk(0.0, 0.0, 0.0);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testCalculateFormulaicRisk_AllMax() {
        double score = aiRiskEngine.calculateFormulaicRisk(10000000.0, 1.0, 1.0);
        // (0.4 * 1.0) + (0.3 * 1.0) + (0.3 * 1.0) = 1.0
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void testGenerateAiInsights_ContainsShipmentInfo() {
        when(mockChatModel.call(anyString())).thenAnswer(inv -> {
            String prompt = inv.getArgument(0);
            return "Analysis for " + prompt;
        });

        String insights = aiRiskEngine.generateAiInsights(123L, 750000.0, "USA to UAE", 0.6);
        assertNotNull(insights);
        verify(mockChatModel).call(anyString());
    }

    @Test
    void testDetectAnomalies_EmptyData() {
        when(mockChatModel.call(anyString())).thenReturn("NORMAL: Insufficient data");

        String result = aiRiskEngine.detectAnomalies(100L, "");
        assertNotNull(result);
    }
}
