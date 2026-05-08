package com.aegisdiamond.risk.service;

import com.aegisdiamond.risk.entity.RiskAssessment;
import com.aegisdiamond.risk.grpc.*;
import com.aegisdiamond.risk.repository.RiskRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskGrpcServiceTest {

    @Mock
    private RiskRepository riskRepository;

    @Mock
    private AiRiskEngine aiRiskEngine;

    @Mock
    private StreamObserver<RiskResponse> riskResponseObserver;

    @Mock
    private StreamObserver<AnomalyResponse> anomalyResponseObserver;

    @Mock
    private StreamObserver<InsightResponse> insightResponseObserver;

    private RiskGrpcService riskGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        riskGrpcService = new RiskGrpcService();
        setPrivateField(riskGrpcService, "riskRepository", riskRepository);
        setPrivateField(riskGrpcService, "aiRiskEngine", aiRiskEngine);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void calculateRiskScore_LowRisk() {
        RiskRequest request = RiskRequest.newBuilder()
                .setShipmentId(100L)
                .setShipmentValue(50000.0)
                .setRoute("Safe Route")
                .setHistoricalRisk(0.1)
                .build();

        when(riskRepository.save(any(RiskAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

        riskGrpcService.calculateRiskScore(request, riskResponseObserver);

        ArgumentCaptor<RiskResponse> captor = ArgumentCaptor.forClass(RiskResponse.class);
        verify(riskResponseObserver).onNext(captor.capture());
        verify(riskResponseObserver).onCompleted();

        RiskResponse response = captor.getValue();
        assertEquals(100L, response.getShipmentId());
        assertEquals("LOW", response.getRiskLevel());
        assertFalse(response.getRequiresApproval());
    }

    @Test
    void calculateRiskScore_HighRiskRoute() {
        RiskRequest request = RiskRequest.newBuilder()
                .setShipmentId(100L)
                .setShipmentValue(500000.0)
                .setRoute("High-Risk Zone")
                .setHistoricalRisk(0.8)
                .build();

        when(riskRepository.save(any(RiskAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

        riskGrpcService.calculateRiskScore(request, riskResponseObserver);

        ArgumentCaptor<RiskResponse> captor = ArgumentCaptor.forClass(RiskResponse.class);
        verify(riskResponseObserver).onNext(captor.capture());

        RiskResponse response = captor.getValue();
        assertTrue(response.getRiskScore() > 0.5);
        assertTrue(response.getRequiresApproval());
    }

    @Test
    void analyzeShipmentRisk_WithAiInsights() {
        RiskRequest request = RiskRequest.newBuilder()
                .setShipmentId(100L)
                .setShipmentValue(100000.0)
                .setRoute("Standard Route")
                .setHistoricalRisk(0.2)
                .build();

        when(aiRiskEngine.generateAiInsights(anyLong(), anyDouble(), anyString(), anyDouble()))
                .thenReturn("AI Insight: Moderate risk due to value.");
        when(riskRepository.save(any(RiskAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

        riskGrpcService.analyzeShipmentRisk(request, riskResponseObserver);

        verify(riskResponseObserver).onNext(any(RiskResponse.class));
        verify(riskResponseObserver).onCompleted();
    }

    @Test
    void detectAnomalies_AnomalyDetected() {
        AnomalyRequest request = AnomalyRequest.newBuilder()
                .setShipmentId(100L)
                .setCurrentData("Route deviation detected")
                .build();

        when(aiRiskEngine.detectAnomalies(anyLong(), anyString()))
                .thenReturn("ANOMALY DETECTED: Route deviation of 50km");

        riskGrpcService.detectAnomalies(request, anomalyResponseObserver);

        ArgumentCaptor<AnomalyResponse> captor = ArgumentCaptor.forClass(AnomalyResponse.class);
        verify(anomalyResponseObserver).onNext(captor.capture());

        AnomalyResponse response = captor.getValue();
        assertTrue(response.getIsAnomaly());
        assertTrue(response.getConfidenceScore() > 0.5);
    }

    @Test
    void detectAnomalies_NoAnomaly() {
        AnomalyRequest request = AnomalyRequest.newBuilder()
                .setShipmentId(100L)
                .setCurrentData("Normal route")
                .build();

        when(aiRiskEngine.detectAnomalies(anyLong(), anyString()))
                .thenReturn("NORMAL: No deviations detected");

        riskGrpcService.detectAnomalies(request, anomalyResponseObserver);

        ArgumentCaptor<AnomalyResponse> captor = ArgumentCaptor.forClass(AnomalyResponse.class);
        verify(anomalyResponseObserver).onNext(captor.capture());
        assertFalse(captor.getValue().getIsAnomaly());
    }

    @Test
    void getRiskInsights_ReturnsInsights() {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setShipmentId(100L);
        assessment.setAiInsights("Test insights");

        when(riskRepository.findByShipmentIdOrderByAssessedAtDesc(100L))
                .thenReturn(List.of(assessment));

        RiskRequest request = RiskRequest.newBuilder().setShipmentId(100L).build();

        riskGrpcService.getRiskInsights(request, insightResponseObserver);

        verify(insightResponseObserver).onNext(any(InsightResponse.class));
        verify(insightResponseObserver).onCompleted();
    }
}
