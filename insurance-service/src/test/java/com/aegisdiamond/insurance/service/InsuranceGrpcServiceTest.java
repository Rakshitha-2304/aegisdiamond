package com.aegisdiamond.insurance.service;

import com.aegisdiamond.insurance.entity.InsuranceClaim;
import com.aegisdiamond.insurance.entity.InsurancePolicy;
import com.aegisdiamond.insurance.grpc.*;
import com.aegisdiamond.insurance.repository.ClaimRepository;
import com.aegisdiamond.insurance.repository.InsuranceRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceGrpcServiceTest {

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ValuationService valuationService;

    @Mock
    private StreamObserver<ValuationResponse> valuationObserver;

    @Mock
    private StreamObserver<PolicyResponse> policyObserver;

    @Mock
    private StreamObserver<ClaimResponse> claimObserver;

    private InsuranceGrpcService insuranceGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        insuranceGrpcService = new InsuranceGrpcService();
        setPrivateField(insuranceGrpcService, "insuranceRepository", insuranceRepository);
        setPrivateField(insuranceGrpcService, "claimRepository", claimRepository);
        setPrivateField(insuranceGrpcService, "valuationService", valuationService);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void calculateDiamondValue_Success() {
        when(valuationService.calculateDiamondValue(10000.0, 2.5, 1.5)).thenReturn(37500.0);

        ValuationRequest request = ValuationRequest.newBuilder()
                .setDiamondId(1L)
                .setBasePrice(10000.0)
                .setCarat(2.5)
                .setQualityMultiplier(1.5)
                .build();

        insuranceGrpcService.calculateDiamondValue(request, valuationObserver);

        ArgumentCaptor<ValuationResponse> captor = ArgumentCaptor.forClass(ValuationResponse.class);
        verify(valuationObserver).onNext(captor.capture());

        ValuationResponse response = captor.getValue();
        assertEquals(37500.0, response.getCalculatedValue(), 0.001);
        assertEquals("USD", response.getCurrency());
    }

    @Test
    void createInsurancePolicy_NewPolicy() {
        when(insuranceRepository.findByShipmentId(100L)).thenReturn(Optional.empty());

        InsurancePolicy saved = new InsurancePolicy();
        saved.setId(1L);
        saved.setShipmentId(100L);
        saved.setStatus("ACTIVE");

        when(insuranceRepository.save(any(InsurancePolicy.class))).thenReturn(saved);

        PolicyRequest request = PolicyRequest.newBuilder()
                .setShipmentId(100L)
                .setCoverageAmount(500000.0)
                .setProvider("Global Insurance")
                .build();

        insuranceGrpcService.createInsurancePolicy(request, policyObserver);

        verify(policyObserver).onNext(any(PolicyResponse.class));
        verify(policyObserver).onCompleted();
    }

    @Test
    void createInsurancePolicy_UpdateExisting() {
        InsurancePolicy existing = new InsurancePolicy();
        existing.setId(1L);
        existing.setShipmentId(100L);

        when(insuranceRepository.findByShipmentId(100L)).thenReturn(Optional.of(existing));
        when(insuranceRepository.save(any(InsurancePolicy.class))).thenReturn(existing);

        PolicyRequest request = PolicyRequest.newBuilder()
                .setShipmentId(100L)
                .setCoverageAmount(600000.0)
                .setProvider("New Provider")
                .build();

        insuranceGrpcService.createInsurancePolicy(request, policyObserver);

        verify(policyObserver).onNext(any(PolicyResponse.class));
    }

    @Test
    void updateInsuranceCoverage_Success() {
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(1L);
        policy.setShipmentId(100L);

        when(insuranceRepository.findByShipmentId(100L)).thenReturn(Optional.of(policy));
        when(insuranceRepository.save(any(InsurancePolicy.class))).thenReturn(policy);

        PolicyRequest request = PolicyRequest.newBuilder()
                .setShipmentId(100L)
                .setCoverageAmount(700000.0)
                .build();

        insuranceGrpcService.updateInsuranceCoverage(request, policyObserver);

        verify(policyObserver).onNext(any(PolicyResponse.class));
        verify(policyObserver).onCompleted();
    }

    @Test
    void updateInsuranceCoverage_PolicyNotFound() {
        when(insuranceRepository.findByShipmentId(999L)).thenReturn(Optional.empty());

        PolicyRequest request = PolicyRequest.newBuilder()
                .setShipmentId(999L)
                .build();

        insuranceGrpcService.updateInsuranceCoverage(request, policyObserver);
        verify(policyObserver).onError(any(Throwable.class));
    }

    @Test
    void claimInsurance_Success() {
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(1L);
        policy.setShipmentId(100L);
        policy.setCoverageAmount(500000.0);
        policy.setStatus("ACTIVE");

        when(insuranceRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(claimRepository.save(any(InsuranceClaim.class))).thenAnswer(inv -> inv.getArgument(0));

        ClaimRequest request = ClaimRequest.newBuilder()
                .setPolicyId(1L)
                .setReason("Diamond lost in transit")
                .setClaimAmount(400000.0)
                .build();

        insuranceGrpcService.claimInsurance(request, claimObserver);

        ArgumentCaptor<ClaimResponse> captor = ArgumentCaptor.forClass(ClaimResponse.class);
        verify(claimObserver).onNext(captor.capture());

        ClaimResponse response = captor.getValue();
        assertEquals("APPROVED", response.getStatus());
        assertTrue(response.getApprovedAmount() > 0);
    }

    @Test
    void getInsuranceDetails_Success() {
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(1L);
        policy.setShipmentId(100L);
        policy.setStatus("ACTIVE");

        when(insuranceRepository.findByShipmentId(100L)).thenReturn(Optional.of(policy));

        ShipmentIdRequest request = ShipmentIdRequest.newBuilder()
                .setShipmentId(100L)
                .build();

        insuranceGrpcService.getInsuranceDetails(request, policyObserver);

        verify(policyObserver).onNext(any(PolicyResponse.class));
        verify(policyObserver).onCompleted();
    }
}
