package com.aegisdiamond.fraud.service;

import com.aegisdiamond.fraud.entity.FraudIncident;
import com.aegisdiamond.fraud.grpc.*;
import com.aegisdiamond.fraud.repository.FraudRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class FraudGrpcService extends FraudServiceGrpc.FraudServiceImplBase {

    @Autowired
    private FraudRepository fraudRepository;

    @Autowired
    private AiFraudEngine aiFraudEngine;

    @Override
    @PreAuthorize("hasAnyRole('SHIPPER', 'INSURANCE_AGENT')")
    public void detectTampering(TamperRequest request, StreamObserver<FraudResponse> responseObserver) {
        boolean isTampered = !request.getCurrentSealState().equalsIgnoreCase("INTACT");
        
        if (isTampered) {
            saveIncident(request.getShipmentId(), "TAMPERING", "Seal " + request.getSealId() + " has been compromised.", 1.0, "COMPROMISED");
        }

        responseObserver.onNext(FraudResponse.newBuilder()
                .setIsFraudulent(isTampered)
                .setProbability(isTampered ? 1.0 : 0.0)
                .setDescription(isTampered ? "Seal integrity violated." : "Seal is intact.")
                .setStatus(isTampered ? "COMPROMISED" : "SECURE")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('INSURANCE_AGENT')")
    public void analyzeFraudPatterns(FraudRequest request, StreamObserver<FraudResponse> responseObserver) {
        String analysis = aiFraudEngine.analyzePatterns(request.getShipmentId(), request.getPayload());
        boolean isFraud = analysis.contains("FRAUD DETECTED");
        double prob = aiFraudEngine.calculateFraudProbability(analysis);

        if (isFraud || prob > 0.5) {
            saveIncident(request.getShipmentId(), "FRAUD_PATTERN", analysis, prob, isFraud ? "COMPROMISED" : "SUSPICIOUS");
        }

        responseObserver.onNext(FraudResponse.newBuilder()
                .setIsFraudulent(isFraud)
                .setProbability(prob)
                .setDescription(analysis)
                .setStatus(isFraud ? "COMPROMISED" : (prob > 0.5 ? "SUSPICIOUS" : "SECURE"))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('INSURANCE_AGENT', 'CUSTOMS_OFFICER')")
    public void flagSuspiciousShipments(FraudRequest request, StreamObserver<FraudResponse> responseObserver) {
        // High-level wrapper for AI flagging
        analyzeFraudPatterns(request, responseObserver);
    }

    @Override
    @PreAuthorize("hasAnyRole('INSURANCE_AGENT', 'CUSTOMS_OFFICER')")
    public void getFraudReports(ShipmentIdRequest request, StreamObserver<FraudReportListResponse> responseObserver) {
        List<FraudIncident> incidents = fraudRepository.findByShipmentIdOrderByDetectedAtDesc(request.getShipmentId());
        
        FraudReportListResponse response = FraudReportListResponse.newBuilder()
                .addAllReports(incidents.stream().map(this::mapToReport).collect(Collectors.toList()))
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void saveIncident(Long shipmentId, String type, String desc, double score, String status) {
        FraudIncident incident = new FraudIncident();
        incident.setShipmentId(shipmentId);
        incident.setType(type);
        incident.setDescription(desc);
        incident.setConfidenceScore(score);
        incident.setStatus(status);
        fraudRepository.save(incident);
    }

    private FraudReport mapToReport(FraudIncident incident) {
        return FraudReport.newBuilder()
                .setId(incident.getId() != null ? incident.getId() : 0L)
                .setShipmentId(incident.getShipmentId() != null ? incident.getShipmentId() : 0L)
                .setType(incident.getType())
                .setDescription(incident.getDescription())
                .setTimestamp(incident.getDetectedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
