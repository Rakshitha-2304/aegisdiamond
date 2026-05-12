package com.aegisdiamond.tracking.service;

import com.aegisdiamond.tracking.entity.TrackingRecord;
import com.aegisdiamond.tracking.grpc.*;
import com.aegisdiamond.tracking.repository.TrackingRepository;
import com.aegisdiamond.tracking.util.LocationUtils;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class TrackingGrpcService extends TrackingServiceGrpc.TrackingServiceImplBase {

    @Autowired
    private TrackingRepository trackingRepository;

    private static final double MAX_ALLOWED_DEVIATION_KM = 50.0; // Example threshold

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void updateLocation(LocationRequest request, StreamObserver<TrackingResponse> responseObserver) {
        TrackingRecord record = new TrackingRecord();
        record.setShipmentId(request.getShipmentId());
        record.setLatitude(request.getLatitude());
        record.setLongitude(request.getLongitude());
        record.setStatus("IN_TRANSIT");

        TrackingRecord saved = trackingRepository.save(record);
        responseObserver.onNext(mapToResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyAuthority('supplier', 'shipper', 'customs_officer', 'insurance_agent')")
    public void getCurrentLocation(ShipmentIdRequest request, StreamObserver<TrackingResponse> responseObserver) {
        trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(request.getId()).ifPresentOrElse(record -> {
            responseObserver.onNext(mapToResponse(record));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Tracking data not found for shipment").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void getTrackingHistory(ShipmentIdRequest request, StreamObserver<TrackingHistoryResponse> responseObserver) {
        List<TrackingRecord> history = trackingRepository.findByShipmentIdOrderByTimestampDesc(request.getId());
        TrackingHistoryResponse response = TrackingHistoryResponse.newBuilder()
                .setShipmentId(request.getId())
                .addAllHistory(history.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyAuthority('shipper', 'insurance_agent')")
    public void detectRouteDeviation(ShipmentIdRequest request, StreamObserver<DeviationResponse> responseObserver) {
        trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(request.getId()).ifPresentOrElse(record -> {
            // Mock expected route check
            double expectedLat = record.getLatitude() + 0.1; // Simulated target
            double expectedLon = record.getLongitude() + 0.1;
            
            double deviation = LocationUtils.calculateDistance(record.getLatitude(), record.getLongitude(), expectedLat, expectedLon);
            boolean isDeviated = deviation > MAX_ALLOWED_DEVIATION_KM;

            responseObserver.onNext(DeviationResponse.newBuilder()
                    .setIsDeviated(isDeviated)
                    .setDistanceInKm(deviation)
                    .setMessage(isDeviated ? "CRITICAL: Shipment deviated from route!" : "Shipment is on track.")
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Tracking data not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void calculateETA(ShipmentIdRequest request, StreamObserver<ETAResponse> responseObserver) {
        trackingRepository.findFirstByShipmentIdOrderByTimestampDesc(request.getId()).ifPresentOrElse(record -> {
            // Simple mock ETA calculation
            responseObserver.onNext(ETAResponse.newBuilder()
                    .setShipmentId(request.getId())
                    .setEstimatedArrival(LocalDateTime.now().plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .setRemainingDistance("120 KM")
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Tracking data not found").asRuntimeException());
        });
    }

    private TrackingResponse mapToResponse(TrackingRecord record) {
        return TrackingResponse.newBuilder()
                .setShipmentId(record.getShipmentId() != null ? record.getShipmentId() : 0L)
                .setLatitude(record.getLatitude())
                .setLongitude(record.getLongitude())
                .setTimestamp(record.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .setStatus(record.getStatus() != null ? record.getStatus() : "UNKNOWN")
                .build();
    }
}
