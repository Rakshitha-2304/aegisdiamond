package com.aegisdiamond.customs.service;

import com.aegisdiamond.customs.entity.CustomsDeclaration;
import com.aegisdiamond.customs.grpc.*;
import com.aegisdiamond.customs.repository.CustomsRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomsGrpcServiceTest {

    @Mock
    private CustomsRepository customsRepository;

    @Mock
    private ComplianceEngine complianceEngine;

    @Mock
    private StreamObserver<CustomsResponse> responseObserver;

    private CustomsGrpcService customsGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        customsGrpcService = new CustomsGrpcService();
        setPrivateField(customsGrpcService, "customsRepository", customsRepository);
        setPrivateField(customsGrpcService, "complianceEngine", complianceEngine);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void validateCustomsDocuments_Valid() {
        when(complianceEngine.validateDocuments(eq("USA"), eq("UAE"), anyList()))
                .thenReturn(true);

        CustomsRequest request = CustomsRequest.newBuilder()
                .setShipmentId(100L)
                .setOriginCountry("USA")
                .setDestinationCountry("UAE")
                .addDocumentIds(1L)
                .addDocumentIds(2L)
                .addDocumentIds(3L)
                .build();

        customsGrpcService.validateCustomsDocuments(request, responseObserver);

        ArgumentCaptor<CustomsResponse> captor = ArgumentCaptor.forClass(CustomsResponse.class);
        verify(responseObserver).onNext(captor.capture());

        CustomsResponse response = captor.getValue();
        assertTrue(response.getIsCompliant());
        assertEquals("DOCUMENTS_VALIDATED", response.getStatus());
    }

    @Test
    void validateCustomsDocuments_Invalid() {
        when(complianceEngine.validateDocuments(eq("USA"), eq("UAE"), anyList()))
                .thenReturn(false);
        when(complianceEngine.getComplianceRequirement("UAE"))
                .thenReturn("Kimberley Process Certificate, Invoice, Packing List, Security Clearance");

        CustomsRequest request = CustomsRequest.newBuilder()
                .setShipmentId(100L)
                .setOriginCountry("USA")
                .setDestinationCountry("UAE")
                .addDocumentIds(1L)
                .build();

        customsGrpcService.validateCustomsDocuments(request, responseObserver);

        ArgumentCaptor<CustomsResponse> captor = ArgumentCaptor.forClass(CustomsResponse.class);
        verify(responseObserver).onNext(captor.capture());

        CustomsResponse response = captor.getValue();
        assertFalse(response.getIsCompliant());
        assertEquals("INVALID_DOCUMENTS", response.getStatus());
    }

    @Test
    void submitCustomsDeclaration_Success() {
        when(complianceEngine.validateDocuments(anyString(), anyString(), anyList()))
                .thenReturn(true);

        CustomsDeclaration saved = new CustomsDeclaration();
        saved.setShipmentId(100L);
        saved.setStatus("SUBMITTED");

        when(customsRepository.findByShipmentId(100L)).thenReturn(Optional.empty());
        when(customsRepository.save(any(CustomsDeclaration.class))).thenReturn(saved);

        CustomsRequest request = CustomsRequest.newBuilder()
                .setShipmentId(100L)
                .setOriginCountry("USA")
                .setDestinationCountry("UK")
                .setDeclarationValue(500000.0)
                .addDocumentIds(1L)
                .build();

        customsGrpcService.submitCustomsDeclaration(request, responseObserver);

        verify(responseObserver).onNext(any(CustomsResponse.class));
        verify(responseObserver).onCompleted();
    }

    @Test
    void approveCustomsClearance_Success() {
        CustomsDeclaration declaration = new CustomsDeclaration();
        declaration.setShipmentId(100L);
        declaration.setCompliant(true);
        declaration.setStatus("SUBMITTED");

        when(customsRepository.findByShipmentId(100L)).thenReturn(Optional.of(declaration));
        when(customsRepository.save(any(CustomsDeclaration.class))).thenReturn(declaration);

        CustomsIdRequest request = CustomsIdRequest.newBuilder()
                .setShipmentId(100L)
                .build();

        customsGrpcService.approveCustomsClearance(request, responseObserver);

        ArgumentCaptor<CustomsResponse> captor = ArgumentCaptor.forClass(CustomsResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("APPROVED", captor.getValue().getStatus());
    }

    @Test
    void approveCustomsClearance_FailsIfNotCompliant() {
        CustomsDeclaration declaration = new CustomsDeclaration();
        declaration.setShipmentId(100L);
        declaration.setCompliant(false);

        when(customsRepository.findByShipmentId(100L)).thenReturn(Optional.of(declaration));

        CustomsIdRequest request = CustomsIdRequest.newBuilder()
                .setShipmentId(100L)
                .build();

        customsGrpcService.approveCustomsClearance(request, responseObserver);
        
        verify(responseObserver).onError(any(StatusRuntimeException.class));
    }

    @Test
    void getComplianceStatus_ReturnsStatus() {
        CustomsDeclaration declaration = new CustomsDeclaration();
        declaration.setShipmentId(100L);
        declaration.setStatus("APPROVED");
        declaration.setCompliant(true);

        when(customsRepository.findByShipmentId(100L)).thenReturn(Optional.of(declaration));

        CustomsIdRequest request = CustomsIdRequest.newBuilder()
                .setShipmentId(100L)
                .build();

        customsGrpcService.getComplianceStatus(request, responseObserver);

        verify(responseObserver).onNext(any(CustomsResponse.class));
        verify(responseObserver).onCompleted();
    }
}
