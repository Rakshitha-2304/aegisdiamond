package com.aegisdiamond.shipping.service;

import com.aegisdiamond.shipping.entity.Shipment;
import com.aegisdiamond.shipping.grpc.*;
import com.aegisdiamond.shipping.repository.ShipmentRepository;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShippingServiceRequirementTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private StreamObserver<ShipmentResponse> responseObserver;

    private ShippingGrpcService shippingGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        shippingGrpcService = new ShippingGrpcService();
        Field field = shippingGrpcService.getClass().getDeclaredField("shipmentRepository");
        field.setAccessible(true);
        field.set(shippingGrpcService, shipmentRepository);
    }

    @Test
    @DisplayName("Requirement: Multi-diamond shipment allowed but cannot be empty")
    void createShipment_EmptyDiamonds_ShouldFail() {
        // Arrange
        ShipmentRequest request = ShipmentRequest.newBuilder()
                .setOrigin("New York")
                .setDestination("London")
                .addAllDiamondIds(Collections.emptyList()) // Empty diamonds
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            shippingGrpcService.createShipment(request, responseObserver);
        });

        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement: Cannot modify after sealing")
    void updateShipmentDetails_AlreadySealed_ShouldFail() {
        // Arrange
        Shipment sealedShipment = new Shipment();
        sealedShipment.setId(1L);
        sealedShipment.setSealed(true);
        sealedShipment.setStatus("SEALED");

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(sealedShipment));

        ShipmentRequest request = ShipmentRequest.newBuilder()
                .setId(1L)
                .setOrigin("Tokyo")
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            shippingGrpcService.updateShipmentDetails(request, responseObserver);
        });

        assertEquals(Status.Code.FAILED_PRECONDITION, exception.getStatus().getCode());
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Requirement: Tamper-proof sealing required (cannot seal twice)")
    void sealShipment_AlreadySealed_ShouldFail() {
        // Arrange
        Shipment alreadySealed = new Shipment();
        alreadySealed.setId(1L);
        alreadySealed.setSealed(true);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(alreadySealed));

        SealRequest request = SealRequest.newBuilder()
                .setShipmentId(1L)
                .setSealId("SEAL-123")
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            shippingGrpcService.sealShipment(request, responseObserver);
        });

        assertEquals(Status.Code.FAILED_PRECONDITION, exception.getStatus().getCode());
        verify(shipmentRepository, never()).save(any());
    }
}
