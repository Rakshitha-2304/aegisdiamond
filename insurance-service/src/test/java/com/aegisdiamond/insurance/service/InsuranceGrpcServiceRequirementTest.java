package com.aegisdiamond.insurance.service;

import com.aegisdiamond.insurance.entity.InsurancePolicy;
import com.aegisdiamond.insurance.grpc.*;
import com.aegisdiamond.insurance.repository.ClaimRepository;
import com.aegisdiamond.insurance.repository.InsuranceRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InsuranceGrpcServiceRequirementTest {

    @Mock
    private InsuranceRepository insuranceRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ValuationService valuationService;

    @Mock
    private StreamObserver<ClaimResponse> claimResponseObserver;

    private InsuranceGrpcService insuranceGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        insuranceGrpcService = new InsuranceGrpcService();
        Field f1 = insuranceGrpcService.getClass().getDeclaredField("insuranceRepository");
        f1.setAccessible(true);
        f1.set(insuranceGrpcService, insuranceRepository);
        Field f2 = insuranceGrpcService.getClass().getDeclaredField("claimRepository");
        f2.setAccessible(true);
        f2.set(insuranceGrpcService, claimRepository);
    }

    @Test
    @DisplayName("Requirement: Coverage limits enforced during claim")
    void claimInsurance_ExceedsCoverage_ShouldBeCapped() {
        // Arrange
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId(1L);
        policy.setCoverageAmount(10000.0); // Limit is 10k
        
        when(insuranceRepository.findById(1L)).thenReturn(Optional.of(policy));
        
        ClaimRequest request = ClaimRequest.newBuilder()
                .setPolicyId(1L)
                .setClaimAmount(15000.0) // Requesting 15k
                .build();

        // Act
        insuranceGrpcService.claimInsurance(request, claimResponseObserver);

        // Assert
        verify(claimRepository).save(argThat(claim -> claim.getApprovedAmount() <= 10000.0));
    }
}
