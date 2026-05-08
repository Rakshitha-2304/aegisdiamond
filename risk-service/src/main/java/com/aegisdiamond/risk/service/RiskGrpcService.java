package com.aegisdiamond.risk.service;

import com.aegisdiamond.risk.entity.RiskAssessment;
import com.aegisdiamond.risk.grpc.*;
import com.aegisdiamond.risk.repository.RiskRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@GrpcService
public class RiskGrpcService extends RiskServiceGrpc.RiskServiceImplBase {

    @Autowired
    private RiskRepository riskRepository;

    @Autowired
    private AiRiskEngine aiRiskEngine;

    @Override
    @PreAuthorize("hasAnyRole('SHIPPER', 'INSURANCE_AGENT')")
    public void calculateRiskScore(RiskRequest request, StreamObserver<RiskResponse> responseObserver) {
        // Mock route risk for formula
        double routeRisk = request.getRoute().contains("High-Risk") ? 0.9 : 0.2;
        double score = aiRiskEngine.calculateFormulaicRisk(request.getShipmentValue(), routeRisk, request.getHistoricalRisk());

        RiskResponse response = mapToRiskResponse(request.getShipmentId(), score);
        
        saveAssessment(request.getShipmentId(), score, response.getRiskLevel(), response.getRequiresApproval(), null);
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('SHIPPER', 'INSURANCE_AGENT')")
    public void analyzeShipmentRisk(RiskRequest request, StreamObserver<RiskResponse> responseObserver) {
        double routeRisk = request.getRoute().contains("High-Risk") ? 0.9 : 0.2;
        double score = aiRiskEngine.calculateFormulaicRisk(request.getShipmentValue(), routeRisk, request.getHistoricalRisk());
        
        String insights = aiRiskEngine.generateAiInsights(request.getShipmentId(), request.getShipmentValue(), request.getRoute(), score);
        
        RiskResponse response = mapToRiskResponse(request.getShipmentId(), score);
        
        saveAssessment(request.getShipmentId(), score, response.getRiskLevel(), response.getRequiresApproval(), insights);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('INSURANCE_AGENT')")
    public void detectAnomalies(AnomalyRequest request, StreamObserver<AnomalyResponse> responseObserver) {
        String analysis = aiRiskEngine.detectAnomalies(request.getShipmentId(), request.getCurrentData());
        boolean isAnomaly = analysis.contains("ANOMALY DETECTED");

        responseObserver.onNext(AnomalyResponse.newBuilder()
                .setIsAnomaly(isAnomaly)
                .setDescription(analysis)
                .setConfidenceScore(isAnomaly ? 0.85 : 0.1)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPPLIER', 'SHIPPER', 'INSURANCE_AGENT', 'CUSTOMS_OFFICER')")
    public void getRiskInsights(RiskRequest request, StreamObserver<InsightResponse> responseObserver) {
        List<RiskAssessment> history = riskRepository.findByShipmentIdOrderByAssessedAtDesc(request.getShipmentId());
        
        String latestInsights = history.isEmpty() ? "No historical insights found." : history.get(0).getAiInsights();
        
        responseObserver.onNext(InsightResponse.newBuilder()
                .setShipmentId(request.getShipmentId())
                .setAiInsights(latestInsights != null ? latestInsights : "AI analysis pending.")
                .addRecommendations("Maintain current route")
                .addRecommendations("Verify seal integrity at next checkpoint")
                .build());
        responseObserver.onCompleted();
    }

    private RiskResponse mapToRiskResponse(long shipmentId, double score) {
        String level;
        boolean approval;

        if (score > 0.8) {
            level = "CRITICAL";
            approval = true;
        } else if (score > 0.5) {
            level = "HIGH";
            approval = true;
        } else if (score > 0.2) {
            level = "MEDIUM";
            approval = false;
        } else {
            level = "LOW";
            approval = false;
        }

        return RiskResponse.newBuilder()
                .setShipmentId(shipmentId)
                .setRiskScore(score)
                .setRiskLevel(level)
                .setRequiresApproval(approval)
                .build();
    }

    private void saveAssessment(long shipmentId, double score, String level, boolean approval, String insights) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.setShipmentId(shipmentId);
        assessment.setRiskScore(score);
        assessment.setRiskLevel(level);
        assessment.setRequiresApproval(approval);
        assessment.setAiInsights(insights);
        riskRepository.save(assessment);
    }
}
