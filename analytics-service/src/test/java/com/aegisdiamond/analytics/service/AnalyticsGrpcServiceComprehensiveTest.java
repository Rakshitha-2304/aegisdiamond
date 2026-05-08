package com.aegisdiamond.analytics.service;

import com.aegisdiamond.analytics.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsGrpcServiceComprehensiveTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Long> longQuery;

    @Mock
    private TypedQuery<Double> doubleQuery;

    @Mock
    private TypedQuery<Object[]> objectArrayQuery;

    @Mock
    private StreamObserver<ShipmentAnalyticsResponse> shipmentObserver;

    @Mock
    private StreamObserver<RiskReportResponse> riskObserver;

    @Mock
    private StreamObserver<RevenueResponse> revenueObserver;

    @Mock
    private StreamObserver<ComplianceReportResponse> complianceObserver;

    private AnalyticsGrpcService analyticsGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        analyticsGrpcService = new AnalyticsGrpcService();
        setPrivateField(analyticsGrpcService, "entityManager", entityManager);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void getShipmentAnalytics_Success() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(100L).thenReturn(50L).thenReturn(25L);

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        analyticsGrpcService.getShipmentAnalytics(request, shipmentObserver);

        ArgumentCaptor<ShipmentAnalyticsResponse> captor = ArgumentCaptor.forClass(ShipmentAnalyticsResponse.class);
        verify(shipmentObserver).onNext(captor.capture());

        ShipmentAnalyticsResponse response = captor.getValue();
        assertEquals(100, response.getTotalShipments());
        assertEquals(50, response.getDeliveredShipments());
        assertEquals(25, response.getInTransitShipments());
        verify(shipmentObserver).onCompleted();
    }

    @Test
    void getRiskReports_Success() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(10L).thenReturn(5L).thenReturn(20L);

        when(entityManager.createQuery(anyString(), eq(Double.class))).thenReturn(doubleQuery);
        when(doubleQuery.getSingleResult()).thenReturn(0.45);

        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(objectArrayQuery);
        when(objectArrayQuery.getResultList()).thenReturn(Collections.emptyList());

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        analyticsGrpcService.getRiskReports(request, riskObserver);

        verify(riskObserver).onNext(any(RiskReportResponse.class));
        verify(riskObserver).onCompleted();
    }

    @Test
    void getRiskReports_WithIncidentTypes() {
        Object[] incident1 = new Object[]{"TAMPERING", 5L};
        Object[] incident2 = new Object[]{"FRAUD_PATTERN", 3L};

        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(10L).thenReturn(5L).thenReturn(8L);

        when(entityManager.createQuery(anyString(), eq(Double.class))).thenReturn(doubleQuery);
        when(doubleQuery.getSingleResult()).thenReturn(0.6);

        when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(objectArrayQuery);
        when(objectArrayQuery.getResultList()).thenReturn(List.of(incident1, incident2));

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        analyticsGrpcService.getRiskReports(request, riskObserver);

        ArgumentCaptor<RiskReportResponse> captor = ArgumentCaptor.forClass(RiskReportResponse.class);
        verify(riskObserver).onNext(captor.capture());

        RiskReportResponse response = captor.getValue();
        assertTrue(response.getIncidentsByTypeMap().containsKey("TAMPERING"));
        assertTrue(response.getIncidentsByTypeMap().containsKey("FRAUD_PATTERN"));
    }

    @Test
    void getRevenueInsights_Success() {
        when(entityManager.createQuery(anyString(), eq(Double.class))).thenReturn(doubleQuery);
        when(doubleQuery.getSingleResult()).thenReturn(5000000.0).thenReturn(1000000.0);

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        analyticsGrpcService.getRevenueInsights(request, revenueObserver);

        ArgumentCaptor<RevenueResponse> captor = ArgumentCaptor.forClass(RevenueResponse.class);
        verify(revenueObserver).onNext(captor.capture());

        RevenueResponse response = captor.getValue();
        assertEquals("USD", response.getCurrency());
        assertTrue(response.getTotalRevenue() > 0);
        verify(revenueObserver).onCompleted();
    }

    @Test
    void getComplianceReports_Success() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(50L).thenReturn(45L);

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        analyticsGrpcService.getComplianceReports(request, complianceObserver);

        ArgumentCaptor<ComplianceReportResponse> captor = ArgumentCaptor.forClass(ComplianceReportResponse.class);
        verify(complianceObserver).onNext(captor.capture());

        ComplianceReportResponse response = captor.getValue();
        assertEquals(50, response.getTotalDeclarations());
        assertEquals(45, response.getCompliantDeclarations());
        assertTrue(response.getComplianceRate() > 0);
        verify(complianceObserver).onCompleted();
    }

    @Test
    void getComplianceReports_NoDeclarations() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.getSingleResult()).thenReturn(null).thenReturn(null);

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        analyticsGrpcService.getComplianceReports(request, complianceObserver);

        verify(complianceObserver).onNext(any(ComplianceReportResponse.class));
    }
}
