package com.aegisdiamond.analytics.service;

import com.aegisdiamond.analytics.grpc.*;
import com.aegisdiamond.shipping.entity.Shipment;
import com.aegisdiamond.risk.entity.RiskAssessment;
import com.aegisdiamond.fraud.entity.FraudIncident;
import com.aegisdiamond.payment.entity.Transaction;
import com.aegisdiamond.insurance.entity.InsurancePolicy;
import com.aegisdiamond.customs.entity.CustomsDeclaration;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GrpcService
public class AnalyticsGrpcService extends AnalyticsServiceGrpc.AnalyticsServiceImplBase {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void getShipmentAnalytics(AnalyticsRequest request, StreamObserver<ShipmentAnalyticsResponse> responseObserver) {
        Long total = entityManager.createQuery("SELECT COUNT(s) FROM Shipment s", Long.class).getSingleResult();
        Long delivered = entityManager.createQuery("SELECT COUNT(s) FROM Shipment s WHERE s.status = 'DELIVERED'", Long.class).getSingleResult();
        Long inTransit = entityManager.createQuery("SELECT COUNT(s) FROM Shipment s WHERE s.status = 'IN_TRANSIT'", Long.class).getSingleResult();

        responseObserver.onNext(ShipmentAnalyticsResponse.newBuilder()
                .setTotalShipments(total != null ? total.intValue() : 0)
                .setDeliveredShipments(delivered != null ? delivered.intValue() : 0)
                .setInTransitShipments(inTransit != null ? inTransit.intValue() : 0)
                .setAverageDeliveryTimeHours(24.5)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'INSURANCE_AGENT')")
    public void getRiskReports(AnalyticsRequest request, StreamObserver<RiskReportResponse> responseObserver) {
        Long highRisk = entityManager.createQuery("SELECT COUNT(r) FROM RiskAssessment r WHERE r.riskLevel = 'HIGH' OR r.riskLevel = 'CRITICAL'", Long.class).getSingleResult();
        Double avgScore = entityManager.createQuery("SELECT AVG(r.riskScore) FROM RiskAssessment r", Double.class).getSingleResult();
        Long incidents = entityManager.createQuery("SELECT COUNT(f) FROM FraudIncident f", Long.class).getSingleResult();

        Map<String, Integer> incidentMap = new HashMap<>();
        List<Object[]> results = entityManager.createQuery("SELECT f.type, COUNT(f) FROM FraudIncident f GROUP BY f.type", Object[].class).getResultList();
        if (results != null) {
            for (Object[] result : results) {
                if (result != null && result.length >= 2 && result[0] != null && result[1] != null) {
                    incidentMap.put(result[0].toString(), ((Long) result[1]).intValue());
                }
            }
        }

        responseObserver.onNext(RiskReportResponse.newBuilder()
                .setHighRiskShipments(highRisk != null ? highRisk.intValue() : 0)
                .setTotalIncidents(incidents != null ? incidents.intValue() : 0)
                .setAverageRiskScore(avgScore != null ? avgScore : 0.0)
                .putAllIncidentsByType(incidentMap)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public void getRevenueInsights(AnalyticsRequest request, StreamObserver<RevenueResponse> responseObserver) {
        Double totalRev = entityManager.createQuery("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'RELEASED' OR t.status = 'CONFIRMED'", Double.class).getSingleResult();
        Double insurance = entityManager.createQuery("SELECT SUM(p.coverageAmount) FROM InsurancePolicy p", Double.class).getSingleResult(); 
        
        double insurancePremiums = insurance != null ? insurance * 0.05 : 0.0;

        responseObserver.onNext(RevenueResponse.newBuilder()
                .setTotalRevenue(totalRev != null ? totalRev : 0.0)
                .setTotalInsurancePremiums(insurancePremiums)
                .setTotalVaultFees(1500.0)
                .setCurrency("USD")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMS_OFFICER')")
    public void getComplianceReports(AnalyticsRequest request, StreamObserver<ComplianceReportResponse> responseObserver) {
        Long total = entityManager.createQuery("SELECT COUNT(c) FROM CustomsDeclaration c", Long.class).getSingleResult();
        Long compliant = entityManager.createQuery("SELECT COUNT(c) FROM CustomsDeclaration c WHERE c.isCompliant = true", Long.class).getSingleResult();
        
        long totalVal = total != null ? total : 0L;
        long compliantVal = compliant != null ? compliant : 0L;
        long nonCompliant = totalVal - compliantVal;

        double rate = totalVal > 0 ? (double) compliantVal / totalVal : 0.0;

        responseObserver.onNext(ComplianceReportResponse.newBuilder()
                .setTotalDeclarations((int) totalVal)
                .setCompliantDeclarations((int) compliantVal)
                .setNonCompliantDeclarations((int) nonCompliant)
                .setComplianceRate(rate)
                .build());
        responseObserver.onCompleted();
    }
}
