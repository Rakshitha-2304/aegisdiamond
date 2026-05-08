package com.aegisdiamond.vault.service;

import com.aegisdiamond.vault.entity.Vault;
import com.aegisdiamond.vault.grpc.*;
import com.aegisdiamond.vault.repository.VaultRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
public class VaultGrpcService extends VaultServiceGrpc.VaultServiceImplBase {

    @Autowired
    private VaultRepository vaultRepository;

    @Autowired
    private GeoSecurityService geoSecurityService;

    @Override
    @PreAuthorize("hasRole('VAULT_MANAGER')")
    public void registerVault(VaultRequest request, StreamObserver<VaultResponse> responseObserver) {
        Vault vault = new Vault();
        vault.setLocation(request.getLocation());
        vault.setCapacity(request.getCapacity());
        vault.setLatitude(request.getLatitude());
        vault.setLongitude(request.getLongitude());

        Vault saved = vaultRepository.save(vault);
        responseObserver.onNext(VaultResponse.newBuilder()
                .setId(saved.getId() != null ? saved.getId() : 0L)
                .setLocation(saved.getLocation())
                .setStatus("REGISTERED")
                .setMessage("Vault registered successfully with capacity: " + saved.getCapacity())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('VAULT_MANAGER')")
    public void storeDiamond(StorageRequest request, StreamObserver<VaultResponse> responseObserver) {
        vaultRepository.findById(request.getVaultId()).ifPresentOrElse(vault -> {
            // Security Checks
            if (!mockMfaCheck(request.getMfaCode())) {
                responseObserver.onError(io.grpc.Status.PERMISSION_DENIED.withDescription("MFA Verification Failed").asRuntimeException());
                return;
            }

            if (!geoSecurityService.isLocationSecure(request.getRequesterLat(), request.getRequesterLon(), vault.getLatitude(), vault.getLongitude())) {
                responseObserver.onError(io.grpc.Status.PERMISSION_DENIED.withDescription("Geo-location security check failed").asRuntimeException());
                return;
            }

            // Capacity Check
            if (vault.isFull()) {
                responseObserver.onError(io.grpc.Status.RESOURCE_EXHAUSTED.withDescription("Vault is at full capacity").asRuntimeException());
                return;
            }

            vault.getDiamondIds().add(request.getDiamondId());
            vaultRepository.save(vault);

            responseObserver.onNext(VaultResponse.newBuilder()
                    .setId(vault.getId() != null ? vault.getId() : 0L)
                    .setStatus("STORED")
                    .setMessage("Diamond " + request.getDiamondId() + " securely stored in vault " + vault.getLocation())
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Vault not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasRole('VAULT_MANAGER')")
    public void retrieveDiamond(StorageRequest request, StreamObserver<VaultResponse> responseObserver) {
        vaultRepository.findById(request.getVaultId()).ifPresentOrElse(vault -> {
            if (!mockMfaCheck(request.getMfaCode())) {
                responseObserver.onError(io.grpc.Status.PERMISSION_DENIED.withDescription("MFA Verification Failed").asRuntimeException());
                return;
            }

            if (!vault.getDiamondIds().contains(request.getDiamondId())) {
                responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found in this vault").asRuntimeException());
                return;
            }

            vault.getDiamondIds().remove(Long.valueOf(request.getDiamondId()));
            vaultRepository.save(vault);

            responseObserver.onNext(VaultResponse.newBuilder()
                    .setId(vault.getId() != null ? vault.getId() : 0L)
                    .setStatus("RETRIEVED")
                    .setMessage("Diamond " + request.getDiamondId() + " retrieved from vault " + vault.getLocation())
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Vault not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasRole('VAULT_MANAGER')")
    public void transferBetweenVaults(TransferRequest request, StreamObserver<VaultResponse> responseObserver) {
        Vault source = vaultRepository.findById(request.getSourceVaultId()).orElse(null);
        Vault destination = vaultRepository.findById(request.getDestinationVaultId()).orElse(null);

        if (source == null || destination == null) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Source or Destination vault not found").asRuntimeException());
            return;
        }

        if (!source.getDiamondIds().contains(request.getDiamondId())) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found in source vault").asRuntimeException());
            return;
        }

        if (destination.isFull()) {
            responseObserver.onError(io.grpc.Status.RESOURCE_EXHAUSTED.withDescription("Destination vault is at full capacity").asRuntimeException());
            return;
        }

        source.getDiamondIds().remove(Long.valueOf(request.getDiamondId()));
        destination.getDiamondIds().add(request.getDiamondId());

        vaultRepository.save(source);
        vaultRepository.save(destination);

        responseObserver.onNext(VaultResponse.newBuilder()
                .setId(destination.getId() != null ? destination.getId() : 0L)
                .setStatus("TRANSFERRED")
                .setMessage("Diamond " + request.getDiamondId() + " transferred from " + source.getLocation() + " to " + destination.getLocation())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('VAULT_MANAGER')")
    public void getVaultInventory(VaultIdRequest request, StreamObserver<InventoryResponse> responseObserver) {
        vaultRepository.findById(request.getId()).ifPresentOrElse(vault -> {
            responseObserver.onNext(InventoryResponse.newBuilder()
                    .setVaultId(vault.getId() != null ? vault.getId() : 0L)
                    .addAllDiamondIds(vault.getDiamondIds())
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Vault not found").asRuntimeException());
        });
    }

    private boolean mockMfaCheck(String code) {
        // In a real system, this would validate against a TOTP or Push notification service
        return code != null && code.length() == 6;
    }
}
