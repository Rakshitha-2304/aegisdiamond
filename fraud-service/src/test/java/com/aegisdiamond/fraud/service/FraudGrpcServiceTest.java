package com.aegisdiamond.fraud.service;

import com.aegisdiamond.fraud.entity.FraudIncident;
import com.aegisdiamond.fraud.grpc.*;
import com.aegisdiamond.fraud.repository.FraudRepository;
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
class FraudGrpcServiceTest {

    @Mock
    private FraudRepository fraudRepository;

    @Mock
    private AiFraudEngine aiFraudEngine;

    @Mock
    private StreamObserver<FraudResponse> fraudResponseObserver;

    @Mock
    private StreamObserver<FraudReportListResponse> reportListObserver;

    private FraudGrpcService fraudGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        fraudGrpcService = new FraudGrpcService();
        // Use reflection to set private fields
        Field field1 = FraudGrpcService.class.getDeclaredField("fraudRepository");
        field1.setAccessible(true);
        field1.set(fraudGrpcService, fraudRepository);

        Field field2 = FraudGrpcService.class.getDeclaredField("aiFraudEngine");
        field2.setAccessible(true);
        field2.set(fraudGrpcService, aiFraudEngine);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field " + fieldName + " not found in class " + target.getClass().getName());
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Test
    void detectTampering_TamperedSeal() {
        TamperRequest request = TamperRequest.newBuilder()
                .setShipmentId(100L)
                .setSealId("SEAL-123")
                .setCurrentSealState("BROKEN")
                .build();

        when(fraudRepository.save(any(FraudIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        fraudGrpcService.detectTampering(request, fraudResponseObserver);

        ArgumentCaptor<FraudResponse> captor = ArgumentCaptor.forClass(FraudResponse.class);
        verify(fraudResponseObserver).onNext(captor.capture());

        FraudResponse response = captor.getValue();
        assertTrue(response.getIsFraudulent());
        assertEquals(1.0, response.getProbability(), 0.001);
        assertEquals("COMPROMISED", response.getStatus());
    }

    @Test
    void detectTampering_IntactSeal() {
        TamperRequest request = TamperRequest.newBuilder()
                .setShipmentId(100L)
                .setSealId("SEAL-123")
                .setCurrentSealState("INTACT")
                .build();

        fraudGrpcService.detectTampering(request, fraudResponseObserver);

        ArgumentCaptor<FraudResponse> captor = ArgumentCaptor.forClass(FraudResponse.class);
        verify(fraudResponseObserver).onNext(captor.capture());

        FraudResponse response = captor.getValue();
        assertFalse(response.getIsFraudulent());
        assertEquals(0.0, response.getProbability(), 0.001);
        assertEquals("SECURE", response.getStatus());
    }

    @Test
    void analyzeFraudPatterns_FraudDetected() {
        FraudRequest request = FraudRequest.newBuilder()
                .setShipmentId(100L)
                .setPayload("Suspicious route pattern detected")
                .build();

        when(aiFraudEngine.analyzePatterns(anyLong(), anyString()))
                .thenReturn("FRAUD DETECTED: Duplicate shipment attempt");
        when(aiFraudEngine.calculateFraudProbability(anyString())).thenReturn(0.95);
        when(fraudRepository.save(any(FraudIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        fraudGrpcService.analyzeFraudPatterns(request, fraudResponseObserver);

        ArgumentCaptor<FraudResponse> captor = ArgumentCaptor.forClass(FraudResponse.class);
        verify(fraudResponseObserver).onNext(captor.capture());

        FraudResponse response = captor.getValue();
        assertTrue(response.getIsFraudulent());
        assertTrue(response.getProbability() > 0.5);
    }

    @Test
    void analyzeFraudPatterns_Secure() {
        FraudRequest request = FraudRequest.newBuilder()
                .setShipmentId(100L)
                .setPayload("Normal shipping pattern")
                .build();

        when(aiFraudEngine.analyzePatterns(anyLong(), anyString()))
                .thenReturn("SECURE: No fraud patterns detected");
        when(aiFraudEngine.calculateFraudProbability(anyString())).thenReturn(0.05);

        fraudGrpcService.analyzeFraudPatterns(request, fraudResponseObserver);

        ArgumentCaptor<FraudResponse> captor = ArgumentCaptor.forClass(FraudResponse.class);
        verify(fraudResponseObserver).onNext(captor.capture());

        FraudResponse response = captor.getValue();
        assertFalse(response.getIsFraudulent());
        assertEquals("SECURE", response.getStatus());
    }

    @Test
    void getFraudReports_ReturnsReports() {
        FraudIncident incident = new FraudIncident();
        incident.setId(1L);
        incident.setShipmentId(100L);
        incident.setType("TAMPERING");

        when(fraudRepository.findByShipmentIdOrderByDetectedAtDesc(100L))
                .thenReturn(List.of(incident));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setShipmentId(100L).build();

        fraudGrpcService.getFraudReports(request, reportListObserver);

        verify(reportListObserver).onNext(any(FraudReportListResponse.class));
        verify(reportListObserver).onCompleted();
    }

    @Test
    void flagSuspiciousShipments_DelegatesToAnalyzePatterns() {
        FraudRequest request = FraudRequest.newBuilder()
                .setShipmentId(100L)
                .setPayload("Suspicious activity")
                .build();

        when(aiFraudEngine.analyzePatterns(anyLong(), anyString()))
                .thenReturn("SUSPICIOUS: Unusual pattern");
        when(aiFraudEngine.calculateFraudProbability(anyString())).thenReturn(0.65);

        fraudGrpcService.flagSuspiciousShipments(request, fraudResponseObserver);

        verify(fraudResponseObserver).onNext(any(FraudResponse.class));
    }
}
