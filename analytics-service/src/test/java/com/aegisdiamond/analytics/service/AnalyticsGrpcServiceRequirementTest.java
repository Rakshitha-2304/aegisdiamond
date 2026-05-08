package com.aegisdiamond.analytics.service;

import com.aegisdiamond.analytics.grpc.*;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnalyticsGrpcServiceRequirementTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StreamObserver<ShipmentAnalyticsResponse> shipmentResponseObserver;

    private AnalyticsGrpcService analyticsGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        analyticsGrpcService = new AnalyticsGrpcService();
        Field field = analyticsGrpcService.getClass().getDeclaredField("entityManager");
        field.setAccessible(true);
        field.set(analyticsGrpcService, entityManager);
    }

    @Test
    @DisplayName("Requirement: Aggregation queries for shipment analytics")
    void getShipmentAnalytics_ShouldQueryDatabase() {
        // Arrange
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(10L);

        AnalyticsRequest request = AnalyticsRequest.newBuilder().build();

        // Act
        analyticsGrpcService.getShipmentAnalytics(request, shipmentResponseObserver);

        // Assert
        verify(entityManager, atLeast(1)).createQuery(anyString(), eq(Long.class));
        verify(shipmentResponseObserver).onNext(argThat(res -> res.getTotalShipments() == 10));
    }
}
