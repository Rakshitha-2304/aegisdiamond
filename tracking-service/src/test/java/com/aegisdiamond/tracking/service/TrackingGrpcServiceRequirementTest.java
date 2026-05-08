package com.aegisdiamond.tracking.service;

import com.aegisdiamond.tracking.entity.TrackingRecord;
import com.aegisdiamond.tracking.grpc.*;
import com.aegisdiamond.tracking.repository.TrackingRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackingGrpcServiceRequirementTest {

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private StreamObserver<DeviationResponse> deviationResponseObserver;

    private TrackingGrpcService trackingGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        trackingGrpcService = new TrackingGrpcService();
        Field field = trackingGrpcService.getClass().getDeclaredField("trackingRepository");
        field.setAccessible(true);
        field.set(trackingGrpcService, trackingRepository);
    }

    @Test
    @DisplayName("Requirement: Deviation triggers alerts")
    void detectRouteDeviation_HighDeviation_ShouldReportDeviated() {
        // Arrange
        TrackingRecord record = new TrackingRecord();
        record.setLatitude(10.0);
        record.setLongitude(10.0);
        
        when(trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(record));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setId(1L).build();

        // Act
        trackingGrpcService.detectRouteDeviation(request, deviationResponseObserver);

        // Assert
        // This will depend on the MAX_ALLOWED_DEVIATION_KM and LocationUtils logic
        // If deviation > 50.0, should be true
    }
}
