package com.aegisdiamond.tracking.service;

import com.aegisdiamond.tracking.entity.TrackingRecord;
import com.aegisdiamond.tracking.grpc.*;
import com.aegisdiamond.tracking.repository.TrackingRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingGrpcServiceTest {

    @Mock
    private TrackingRepository trackingRepository;

    @Mock
    private StreamObserver<TrackingResponse> trackingResponseObserver;

    @Mock
    private StreamObserver<TrackingHistoryResponse> historyResponseObserver;

    @Mock
    private StreamObserver<DeviationResponse> deviationResponseObserver;

    @Mock
    private StreamObserver<ETAResponse> etaResponseObserver;

    private TrackingGrpcService trackingGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        trackingGrpcService = new TrackingGrpcService();
        setPrivateField(trackingGrpcService, "trackingRepository", trackingRepository);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void updateLocation_Success() {
        TrackingRecord saved = new TrackingRecord();
        saved.setId(1L);
        saved.setShipmentId(100L);
        saved.setLatitude(40.7128);
        saved.setLongitude(-74.0060);
        saved.setStatus("IN_TRANSIT");

        when(trackingRepository.save(any(TrackingRecord.class))).thenReturn(saved);

        LocationRequest request = LocationRequest.newBuilder()
                .setShipmentId(100L)
                .setLatitude(40.7128)
                .setLongitude(-74.0060)
                .build();

        trackingGrpcService.updateLocation(request, trackingResponseObserver);

        ArgumentCaptor<TrackingResponse> captor = ArgumentCaptor.forClass(TrackingResponse.class);
        verify(trackingResponseObserver).onNext(captor.capture());
        verify(trackingResponseObserver).onCompleted();

        TrackingResponse response = captor.getValue();
        assertEquals(100L, response.getShipmentId());
        assertEquals(40.7128, response.getLatitude(), 0.001);
    }

    @Test
    void getCurrentLocation_Success() {
        TrackingRecord record = new TrackingRecord();
        record.setId(1L);
        record.setShipmentId(100L);
        record.setLatitude(40.7128);
        record.setLongitude(-74.0060);
        record.setTimestamp(LocalDateTime.now());

        when(trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(100L))
                .thenReturn(Optional.of(record));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setId(100L).build();

        trackingGrpcService.getCurrentLocation(request, trackingResponseObserver);

        verify(trackingResponseObserver).onNext(any(TrackingResponse.class));
        verify(trackingResponseObserver).onCompleted();
    }

    @Test
    void getCurrentLocation_NotFound() {
        when(trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(999L))
                .thenReturn(Optional.empty());

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setId(999L).build();

        assertThrows(StatusRuntimeException.class, () ->
                trackingGrpcService.getCurrentLocation(request, trackingResponseObserver));
    }

    @Test
    void getTrackingHistory_ReturnsRecords() {
        TrackingRecord record = new TrackingRecord();
        record.setShipmentId(100L);

        when(trackingRepository.findByShipmentIdOrderByTimestampDesc(100L))
                .thenReturn(List.of(record));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setId(100L).build();

        trackingGrpcService.getTrackingHistory(request, historyResponseObserver);

        ArgumentCaptor<TrackingHistoryResponse> captor = ArgumentCaptor.forClass(TrackingHistoryResponse.class);
        verify(historyResponseObserver).onNext(captor.capture());
        assertFalse(captor.getValue().getHistoryList().isEmpty());
    }

    @Test
    void detectRouteDeviation_DeviationDetected() {
        TrackingRecord record = new TrackingRecord();
        record.setShipmentId(100L);
        record.setLatitude(40.7128);
        record.setLongitude(-74.0060);

        when(trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(100L))
                .thenReturn(Optional.of(record));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setId(100L).build();

        trackingGrpcService.detectRouteDeviation(request, deviationResponseObserver);

        ArgumentCaptor<DeviationResponse> captor = ArgumentCaptor.forClass(DeviationResponse.class);
        verify(deviationResponseObserver).onNext(captor.capture());
        assertTrue(captor.getValue().getIsDeviated());
    }

    @Test
    void calculateETA_Success() {
        TrackingRecord record = new TrackingRecord();
        record.setShipmentId(100L);

        when(trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(100L))
                .thenReturn(Optional.of(record));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder().setId(100L).build();

        trackingGrpcService.calculateETA(request, etaResponseObserver);

        verify(etaResponseObserver).onNext(any(ETAResponse.class));
        verify(etaResponseObserver).onCompleted();
    }
}
