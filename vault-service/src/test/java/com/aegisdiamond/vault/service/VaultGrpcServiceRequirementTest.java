package com.aegisdiamond.vault.service;

import com.aegisdiamond.vault.entity.Vault;
import com.aegisdiamond.vault.grpc.*;
import com.aegisdiamond.vault.repository.VaultRepository;
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
public class VaultGrpcServiceRequirementTest {

    @Mock
    private VaultRepository vaultRepository;

    @Mock
    private GeoSecurityService geoSecurityService;

    @Mock
    private StreamObserver<VaultResponse> responseObserver;

    private VaultGrpcService vaultGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        vaultGrpcService = new VaultGrpcService();
        setField(vaultGrpcService, "vaultRepository", vaultRepository);
        setField(vaultGrpcService, "geoSecurityService", geoSecurityService);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("Requirement: Vault capacity constraints")
    void storeDiamond_VaultFull_ShouldFail() {
        // Arrange
        Vault fullVault = mock(Vault.class);
        when(fullVault.isFull()).thenReturn(true);
        when(vaultRepository.findById(1L)).thenReturn(Optional.of(fullVault));
        
        // Mock geo and mfa to pass
        when(geoSecurityService.isLocationSecure(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(true);

        StorageRequest request = StorageRequest.newBuilder()
                .setVaultId(1L)
                .setDiamondId(101L)
                .setMfaCode("123456")
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            vaultGrpcService.storeDiamond(request, responseObserver);
        });

        assertEquals(Status.Code.RESOURCE_EXHAUSTED, exception.getStatus().getCode());
    }

    @Test
    @DisplayName("Requirement: Multi-factor authentication for access")
    void storeDiamond_InvalidMfa_ShouldFail() {
        // Arrange
        Vault vault = new Vault();
        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));

        StorageRequest request = StorageRequest.newBuilder()
                .setVaultId(1L)
                .setMfaCode("123") // Too short, fails mock check
                .build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            vaultGrpcService.storeDiamond(request, responseObserver);
        });

        assertEquals(Status.Code.PERMISSION_DENIED, exception.getStatus().getCode());
    }
}
