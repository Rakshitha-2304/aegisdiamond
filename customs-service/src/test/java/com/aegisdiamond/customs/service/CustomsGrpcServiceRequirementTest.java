package com.aegisdiamond.customs.service;

import com.aegisdiamond.customs.entity.CustomsDeclaration;
import com.aegisdiamond.customs.grpc.*;
import com.aegisdiamond.customs.repository.CustomsRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomsGrpcServiceRequirementTest {

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
        Field f1 = customsGrpcService.getClass().getDeclaredField("customsRepository");
        f1.setAccessible(true);
        f1.set(customsGrpcService, customsRepository);
        Field f2 = customsGrpcService.getClass().getDeclaredField("complianceEngine");
        f2.setAccessible(true);
        f2.set(customsGrpcService, complianceEngine);
    }

    @Test
    @DisplayName("Requirement: Cannot approve non-compliant declaration")
    void approveCustomsClearance_NonCompliant_ShouldFail() {
        // Arrange
        CustomsDeclaration declaration = new CustomsDeclaration();
        declaration.setCompliant(false);
        
        when(customsRepository.findByShipmentId(1L)).thenReturn(Optional.of(declaration));

        CustomsIdRequest request = CustomsIdRequest.newBuilder().setShipmentId(1L).build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            customsGrpcService.approveCustomsClearance(request, responseObserver);
        });

        assertEquals(Status.Code.FAILED_PRECONDITION, exception.getStatus().getCode());
    }
}
