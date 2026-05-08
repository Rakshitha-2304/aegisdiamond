package com.aegisdiamond.shipping.service;

import com.aegisdiamond.shipping.entity.Shipment;
import com.aegisdiamond.shipping.grpc.*;
import com.aegisdiamond.shipping.repository.ShipmentRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShippingGrpcServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private StreamObserver<ShipmentResponse> responseObserver;

    @Mock
    private StreamObserver<SecurityResponse> securityResponseObserver;

    private ShippingGrpcService shippingGrpcService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        shippingGrpcService = new ShippingGrpcService();
        // Use reflection to set the private field
        try {
            Field field = ShippingGrpcService.class.getDeclaredField("shipmentRepository");
            field.setAccessible(true);
            field.set(shippingGrpcService, shipmentRepository);
            // Verify it was set
            Object value = field.get(shippingGrpcService);
            if (value == null) {
                throw new RuntimeException("Failed to set shipmentRepository - value is null");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set shipmentRepository", e);
        }
    }

    @Test
    void createShipment_Success() {
        Shipment saved = new Shipment();
        saved.setId(1L);
        saved.setOrigin("New York");
        saved.setDestination("London");
        saved.setStatus("CREATED");

        when(shipmentRepository.save(any(Shipment.class))).thenReturn(saved);

        ShipmentRequest request = ShipmentRequest.newBuilder()
                .setOrigin("New York")
                .setDestination("London")
                .addAllDiamondIds(java.util.List.of(1L, 2L))
                .setShipperId(100L)
                .build();

        shippingGrpcService.createShipment(request, responseObserver);

        ArgumentCaptor<ShipmentResponse> captor = ArgumentCaptor.forClass(ShipmentResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        ShipmentResponse response = captor.getValue();
        assertEquals("CREATED", response.getStatus());
        assertEquals("New York", response.getOrigin());
    }

    @Test
    void updateShipmentDetails_Success() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setOrigin("New York");
        shipment.setStatus("CREATED");
        shipment.setSealed(false);

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        ShipmentRequest request = ShipmentRequest.newBuilder()
                .setId(1L)
                .setOrigin("Updated City")
                .setDestination("Paris")
                .build();

        shippingGrpcService.updateShipmentDetails(request, responseObserver);

        verify(responseObserver).onNext(any(ShipmentResponse.class));
        verify(responseObserver).onCompleted();
    }

    @Test
    void updateShipmentDetails_FailsWhenSealed() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setSealed(true);
        shipment.setStatus("SEALED");

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        ShipmentRequest request = ShipmentRequest.newBuilder()
                .setId(1L)
                .setOrigin("New City")
                .build();

        shippingGrpcService.updateShipmentDetails(request, responseObserver);

        verify(responseObserver).onError(any(Throwable.class));
    }

    @Test
    void assignSecureContainer_Success() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setOrigin("New York");
        shipment.setDestination("London");

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        ContainerRequest request = ContainerRequest.newBuilder()
                .setShipmentId(1L)
                .setContainerId("CONT-123")
                .build();

        shippingGrpcService.assignSecureContainer(request, responseObserver);

        ArgumentCaptor<ShipmentResponse> captor = ArgumentCaptor.forClass(ShipmentResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("VERIFIED", captor.getValue().getStatus());
    }

    @Test
    void sealShipment_Success() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setOrigin("New York");
        shipment.setDestination("London");

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(shipment);

        SealRequest request = SealRequest.newBuilder()
                .setShipmentId(1L)
                .setSealId("SEAL-456")
                .build();

        shippingGrpcService.sealShipment(request, responseObserver);

        ArgumentCaptor<ShipmentResponse> captor = ArgumentCaptor.forClass(ShipmentResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("SEALED", captor.getValue().getStatus());
        assertTrue(captor.getValue().getIsSealed());
    }

    @Test
    void validateShipmentSecurity_Valid() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setSealed(true);
        shipment.setContainerId("CONT-123");
        shipment.setOrigin("New York");
        shipment.setDestination("London");

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        SecurityRequest request = SecurityRequest.newBuilder()
                .setShipmentId(1L)
                .build();

        shippingGrpcService.validateShipmentSecurity(request, securityResponseObserver);

        ArgumentCaptor<SecurityResponse> captor = ArgumentCaptor.forClass(SecurityResponse.class);
        verify(securityResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getIsValid());
    }

    @Test
    void validateShipmentSecurity_Invalid() {
        Shipment shipment = new Shipment();
        shipment.setId(1L);
        shipment.setSealed(false);
        shipment.setContainerId(null);
        shipment.setOrigin("New York");
        shipment.setDestination("London");

        when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));

        SecurityRequest request = SecurityRequest.newBuilder()
                .setShipmentId(1L)
                .build();

        shippingGrpcService.validateShipmentSecurity(request, securityResponseObserver);

        ArgumentCaptor<SecurityResponse> captor = ArgumentCaptor.forClass(SecurityResponse.class);
        verify(securityResponseObserver).onNext(captor.capture());
        assertFalse(captor.getValue().getIsValid());
    }
}
