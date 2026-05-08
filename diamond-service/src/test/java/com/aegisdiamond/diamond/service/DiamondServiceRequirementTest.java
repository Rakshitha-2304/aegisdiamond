package com.aegisdiamond.diamond.service;

import com.aegisdiamond.diamond.entity.Diamond;
import com.aegisdiamond.diamond.grpc.*;
import com.aegisdiamond.diamond.repository.DiamondRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiamondServiceRequirementTest {

    @Mock
    private DiamondRepository diamondRepository;

    @Mock
    private StreamObserver<DiamondResponse> responseObserver;

    private DiamondGrpcService diamondGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        diamondGrpcService = new DiamondGrpcService();
        Field field = diamondGrpcService.getClass().getDeclaredField("diamondRepository");
        field.setAccessible(true);
        field.set(diamondGrpcService, diamondRepository);
    }

    @Test
    @DisplayName("Requirement: Unique certificate ID required")
    void registerDiamond_DuplicateCertificate_ShouldFail() {
        // Arrange
        DiamondRequest request = DiamondRequest.newBuilder()
                .setCut("Excellent")
                .setClarity("VVS1")
                .setColor("D")
                .setCarat(1.5)
                .setCertificateId(9999L)
                .build();

        when(diamondRepository.findByCertificateId(9999L)).thenReturn(Optional.of(new Diamond()));
        
        // Act
        diamondGrpcService.registerDiamond(request, responseObserver);

        // Assert
        ArgumentCaptor<StatusRuntimeException> captor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(responseObserver).onError(captor.capture());
        assertEquals(Status.Code.ALREADY_EXISTS, captor.getValue().getStatus().getCode());
        verify(diamondRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement: 4Cs (cut, clarity, color, carat) mandatory")
    void registerDiamond_MissingMandatoryFields_ShouldFail() {
        // Arrange
        DiamondRequest request = DiamondRequest.newBuilder()
                .setCut("") // Missing cut
                .setClarity("VVS1")
                .setColor("D")
                .setCarat(0.0)
                .setCertificateId(1001L)
                .build();
        
        // Act
        diamondGrpcService.registerDiamond(request, responseObserver);

        // Assert
        ArgumentCaptor<StatusRuntimeException> captor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(responseObserver).onError(captor.capture());
        assertEquals(Status.Code.INVALID_ARGUMENT, captor.getValue().getStatus().getCode());
        verify(diamondRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement: Immutable audit trail - status cannot be bypassed")
    void registerDiamond_ManualStatusSet_ShouldBeOverridden() {
        // Arrange
        // (Assuming the proto allowed setting status, but it doesn't in DiamondRequest)
        // Let's test if the service correctly sets the initial status to REGISTERED regardless of input
        
        DiamondRequest request = DiamondRequest.newBuilder()
                .setCut("Excellent")
                .setClarity("VVS1")
                .setColor("D")
                .setCarat(1.0)
                .setCertificateId(1005L)
                .build();

        ArgumentCaptor<Diamond> diamondCaptor = ArgumentCaptor.forClass(Diamond.class);
        when(diamondRepository.save(diamondCaptor.capture())).thenReturn(new Diamond());

        // Act
        diamondGrpcService.registerDiamond(request, responseObserver);

        // Assert
        assertEquals("REGISTERED", diamondCaptor.getValue().getStatus());
    }
}
