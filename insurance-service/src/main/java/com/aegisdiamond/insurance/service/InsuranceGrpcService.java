package com.aegisdiamond.insurance.service;

import com.aegisdiamond.insurance.entity.InsuranceClaim;
import com.aegisdiamond.insurance.entity.InsurancePolicy;
import com.aegisdiamond.insurance.grpc.*;
import com.aegisdiamond.insurance.repository.ClaimRepository;
import com.aegisdiamond.insurance.repository.InsuranceRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
public class InsuranceGrpcService extends InsuranceServiceGrpc.InsuranceServiceImplBase {

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ValuationService valuationService;

    @Override
    @PreAuthorize("hasRole('INSURANCE_AGENT')")
    public void calculateDiamondValue(ValuationRequest request, StreamObserver<ValuationResponse> responseObserver) {
        double value = valuationService.calculateDiamondValue(request.getBasePrice(), request.getCarat(), request.getQualityMultiplier());
        
        responseObserver.onNext(ValuationResponse.newBuilder()
                .setDiamondId(request.getDiamondId())
                .setCalculatedValue(value)
                .setCurrency("USD")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('INSURANCE_AGENT')")
    public void createInsurancePolicy(PolicyRequest request, StreamObserver<PolicyResponse> responseObserver) {
        InsurancePolicy policy = insuranceRepository.findByShipmentId(request.getShipmentId())
                .orElse(new InsurancePolicy());
        
        policy.setShipmentId(request.getShipmentId());
        policy.setCoverageAmount(request.getCoverageAmount());
        policy.setProvider(request.getProvider());
        policy.setStatus("ACTIVE");

        InsurancePolicy saved = insuranceRepository.save(policy);
        responseObserver.onNext(mapToPolicyResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('INSURANCE_AGENT')")
    public void updateInsuranceCoverage(PolicyRequest request, StreamObserver<PolicyResponse> responseObserver) {
        insuranceRepository.findByShipmentId(request.getShipmentId()).ifPresentOrElse(policy -> {
            policy.setCoverageAmount(request.getCoverageAmount());
            InsurancePolicy saved = insuranceRepository.save(policy);
            responseObserver.onNext(mapToPolicyResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Policy not found for shipment").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPPLIER', 'INSURANCE_AGENT')")
    public void claimInsurance(ClaimRequest request, StreamObserver<ClaimResponse> responseObserver) {
        insuranceRepository.findById(request.getPolicyId()).ifPresentOrElse(policy -> {
            InsuranceClaim claim = new InsuranceClaim();
            claim.setPolicyId(policy.getId());
            claim.setReason(request.getReason());
            claim.setClaimAmount(request.getClaimAmount());
            
            // Simple logic: Approve 90% of claim if within coverage
            double approved = Math.min(request.getClaimAmount() * 0.9, policy.getCoverageAmount());
            claim.setApprovedAmount(approved);
            claim.setStatus("APPROVED");

            InsuranceClaim saved = claimRepository.save(claim);
            
            policy.setStatus("CLAIMED");
            insuranceRepository.save(policy);

            responseObserver.onNext(ClaimResponse.newBuilder()
                    .setClaimId(saved.getId() != null ? saved.getId() : 0L)
                    .setStatus(saved.getStatus())
                    .setApprovedAmount(saved.getApprovedAmount())
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Policy not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPPLIER', 'INSURANCE_AGENT')")
    public void getInsuranceDetails(ShipmentIdRequest request, StreamObserver<PolicyResponse> responseObserver) {
        insuranceRepository.findByShipmentId(request.getShipmentId()).ifPresentOrElse(policy -> {
            responseObserver.onNext(mapToPolicyResponse(policy));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Policy not found for shipment").asRuntimeException());
        });
    }

    private PolicyResponse mapToPolicyResponse(InsurancePolicy policy) {
        return PolicyResponse.newBuilder()
                .setPolicyId(policy.getId() != null ? policy.getId() : 0L)
                .setShipmentId(policy.getShipmentId() != null ? policy.getShipmentId() : 0L)
                .setCoverageAmount(policy.getCoverageAmount())
                .setStatus(policy.getStatus())
                .setProvider(policy.getProvider() != null ? policy.getProvider() : "")
                .build();
    }
}
