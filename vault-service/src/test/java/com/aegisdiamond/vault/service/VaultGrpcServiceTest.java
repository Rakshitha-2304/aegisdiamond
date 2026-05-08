package com.aegisdiamond.vault.service;

import com.aegisdiamond.vault.entity.Vault;
import com.aegisdiamond.vault.grpc.*;
import com.aegisdiamond.vault.repository.VaultRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultGrpcServiceTest {

    @Mock
    private VaultRepository vaultRepository;

    @Mock
    private GeoSecurityService geoSecurityService;

    @Mock
    private StreamObserver<VaultResponse> responseObserver;

    @Mock
    private StreamObserver<InventoryResponse> inventoryResponseObserver;

    private VaultGrpcService vaultGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        vaultGrpcService = new VaultGrpcService();
        setPrivateField(vaultGrpcService, "vaultRepository", vaultRepository);
        setPrivateField(vaultGrpcService, "geoSecurityService", geoSecurityService);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void registerVault_Success() {
        Vault saved = new Vault();
        saved.setId(1L);
        saved.setLocation("New York Vault");
        saved.setCapacity(1000);

        when(vaultRepository.save(any(Vault.class))).thenReturn(saved);

        VaultRequest request = VaultRequest.newBuilder()
                .setLocation("New York Vault")
                .setCapacity(1000)
                .setLatitude(40.7128)
                .setLongitude(-74.0060)
                .build();

        vaultGrpcService.registerVault(request, responseObserver);

        ArgumentCaptor<VaultResponse> captor = ArgumentCaptor.forClass(VaultResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("REGISTERED", captor.getValue().getStatus());
    }

    @Test
    void storeDiamond_Success() {
        Vault vault = new Vault();
        vault.setId(1L);
        vault.setLatitude(40.7128);
        vault.setLongitude(-74.0060);
        vault.setCapacity(1000);
        vault.setDiamondIds(new ArrayList<>());

        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));
        when(geoSecurityService.isLocationSecure(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(true);

        StorageRequest request = StorageRequest.newBuilder()
                .setVaultId(1L)
                .setDiamondId(100L)
                .setMfaCode("123456")
                .setRequesterLat(40.7128)
                .setRequesterLon(-74.0060)
                .build();

        vaultGrpcService.storeDiamond(request, responseObserver);

        ArgumentCaptor<VaultResponse> captor = ArgumentCaptor.forClass(VaultResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("STORED", captor.getValue().getStatus());
    }

    @Test
    void storeDiamond_MfaCheckFails() {
        Vault vault = new Vault();
        vault.setId(1L);

        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));

        StorageRequest request = StorageRequest.newBuilder()
                .setVaultId(1L)
                .setDiamondId(100L)
                .setMfaCode("123")  // Invalid MFA (not 6 chars)
                .build();

        assertThrows(StatusRuntimeException.class, () ->
                vaultGrpcService.storeDiamond(request, responseObserver));
    }

    @Test
    void storeDiamond_GeoSecurityFails() {
        Vault vault = new Vault();
        vault.setId(1L);
        vault.setLatitude(40.7128);
        vault.setLongitude(-74.0060);

        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));
        when(geoSecurityService.isLocationSecure(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(false);

        StorageRequest request = StorageRequest.newBuilder()
                .setVaultId(1L)
                .setDiamondId(100L)
                .setMfaCode("123456")
                .setRequesterLat(50.0)  // Far from vault
                .setRequesterLon(50.0)
                .build();

        assertThrows(StatusRuntimeException.class, () ->
                vaultGrpcService.storeDiamond(request, responseObserver));
    }

    @Test
    void retrieveDiamond_Success() {
        Vault vault = new Vault();
        vault.setId(1L);
        vault.setDiamondIds(new ArrayList<>(List.of(100L)));

        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));
        when(vaultRepository.save(any(Vault.class))).thenReturn(vault);

        StorageRequest request = StorageRequest.newBuilder()
                .setVaultId(1L)
                .setDiamondId(100L)
                .setMfaCode("123456")
                .build();

        vaultGrpcService.retrieveDiamond(request, responseObserver);

        ArgumentCaptor<VaultResponse> captor = ArgumentCaptor.forClass(VaultResponse.class);
        verify(responseObserver).onNext(captor.capture());
        assertEquals("RETRIEVED", captor.getValue().getStatus());
    }

    @Test
    void transferBetweenVaults_Success() {
        Vault source = new Vault();
        source.setId(1L);
        source.setLocation("Vault A");
        source.setDiamondIds(new ArrayList<>(List.of(100L)));

        Vault destination = new Vault();
        destination.setId(2L);
        destination.setLocation("Vault B");
        destination.setDiamondIds(new ArrayList<>());

        when(vaultRepository.findById(1L)).thenReturn(Optional.of(source));
        when(vaultRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(vaultRepository.save(any(Vault.class))).thenReturn(source, destination);

        TransferRequest request = TransferRequest.newBuilder()
                .setSourceVaultId(1L)
                .setDestinationVaultId(2L)
                .setDiamondId(100L)
                .build();

        vaultGrpcService.transferBetweenVaults(request, responseObserver);

        verify(responseObserver).onNext(any(VaultResponse.class));
        verify(responseObserver).onCompleted();
    }

    @Test
    void getVaultInventory_Success() {
        Vault vault = new Vault();
        vault.setId(1L);
        vault.setDiamondIds(new ArrayList<>(List.of(100L, 200L)));

        when(vaultRepository.findById(1L)).thenReturn(Optional.of(vault));

        VaultIdRequest request = VaultIdRequest.newBuilder().setId(1L).build();

        vaultGrpcService.getVaultInventory(request, inventoryResponseObserver);

        ArgumentCaptor<InventoryResponse> captor = ArgumentCaptor.forClass(InventoryResponse.class);
        verify(inventoryResponseObserver).onNext(captor.capture());
        assertEquals(2, captor.getValue().getDiamondIdsList().size());
    }
}
