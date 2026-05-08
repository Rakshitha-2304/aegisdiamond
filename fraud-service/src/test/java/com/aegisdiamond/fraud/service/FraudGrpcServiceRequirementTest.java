package com.aegisdiamond.fraud.service;

import com.aegisdiamond.fraud.entity.FraudIncident;
import com.aegisdiamond.fraud.grpc.*;
import com.aegisdiamond.fraud.repository.FraudRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FraudGrpcServiceRequirementTest {

    @Mock
    private FraudRepository fraudRepository;

    @Mock
    private AiFraudEngine aiFraudEngine;

    @Mock
    private StreamObserver<FraudResponse> responseObserver;

    private FraudGrpcService fraudGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        fraudGrpcService = new FraudGrpcService();
        Field f1 = fraudGrpcService.getClass().getDeclaredField("fraudRepository");
        f1.setAccessible(true);
        f1.set(fraudGrpcService, fraudRepository);
        Field f2 = fraudGrpcService.getClass().getDeclaredField("aiFraudEngine");
        f2.setAccessible(true);
        f2.set(fraudGrpcService, aiFraudEngine);
    }

    @Test
    @DisplayName("Requirement: Seal integrity validation")
    void detectTampering_SealCompromised_ShouldFlagFraud() {
        // Arrange
        TamperRequest request = TamperRequest.newBuilder()
                .setShipmentId(1L)
                .setSealId("SEAL-123")
                .setCurrentSealState("BROKEN") // Tampered
                .build();

        // Act
        fraudGrpcService.detectTampering(request, responseObserver);

        // Assert
        verify(responseObserver).onNext(argThat(res -> res.getIsFraudulent()));
        verify(fraudRepository).save(any(FraudIncident.class));
    }

    @Test
    @DisplayName("Requirement: Duplicate shipment detection")
    void analyzeFraudPatterns_DuplicateShipment_ShouldFlagFraud() {
        // Arrange
        FraudRequest request = FraudRequest.newBuilder()
                .setShipmentId(1L)
                .setPayload("DUPLICATE_SHIPMENT_DETECTED")
                .build();

        when(aiFraudEngine.analyzePatterns(anyLong(), anyString())).thenReturn("FRAUD DETECTED: DUPLICATE SHIPMENT");
        when(aiFraudEngine.calculateFraudProbability(anyString())).thenReturn(0.95);

        // Act
        fraudGrpcService.analyzeFraudPatterns(request, responseObserver);

        // Assert
        verify(responseObserver).onNext(argThat(res -> res.getIsFraudulent()));
    }
}
