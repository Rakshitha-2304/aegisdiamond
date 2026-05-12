package com.aegisdiamond.diamond.service;

import com.aegisdiamond.diamond.entity.Diamond;
import com.aegisdiamond.diamond.grpc.*;
import com.aegisdiamond.diamond.repository.DiamondRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class DiamondGrpcService extends DiamondServiceGrpc.DiamondServiceImplBase {

    @Autowired
    private DiamondRepository diamondRepository;

    @Override
    @PreAuthorize("hasAuthority('supplier')")
    public void registerDiamond(DiamondRequest request, StreamObserver<DiamondResponse> responseObserver) {
        // Requirement: 4Cs (cut, clarity, color, carat) mandatory
        if (request.getCut().isEmpty() || request.getClarity().isEmpty() || 
            request.getColor().isEmpty() || request.getCarat() <= 0) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("4Cs (cut, clarity, color, carat) are mandatory and carat must be positive")
                    .asRuntimeException());
            return;
        }

        // Requirement: Unique certificate ID required if provided
        Long certId = request.getCertificateId() > 0 ? request.getCertificateId() : null;
        if (certId != null && diamondRepository.findByCertificateId(certId).isPresent()) {
            responseObserver.onError(io.grpc.Status.ALREADY_EXISTS
                    .withDescription("Diamond with certificate ID " + certId + " already exists")
                    .asRuntimeException());
            return;
        }

        Diamond diamond = new Diamond();
        diamond.setCut(request.getCut());
        diamond.setClarity(request.getClarity());
        diamond.setColor(request.getColor());
        diamond.setCarat(request.getCarat());
        diamond.setCertificateId(certId);
        diamond.setOwnerId(request.getOwnerId());
        diamond.setStatus(certId != null ? "CERTIFIED" : "REGISTERED");

        Diamond saved = diamondRepository.save(diamond);
        responseObserver.onNext(mapToResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAuthority('supplier')")
    public void updateDiamondDetails(DiamondRequest request, StreamObserver<DiamondResponse> responseObserver) {
        diamondRepository.findById(request.getId()).ifPresentOrElse(diamond -> {
            boolean isCertified = diamond.getCertificateId() != null && diamond.getCertificateId() > 0;
            
            if (isCertified) {
                // Check if certified attributes are being changed
                if (!diamond.getCut().equals(request.getCut()) ||
                    !diamond.getClarity().equals(request.getClarity()) ||
                    !diamond.getColor().equals(request.getColor()) ||
                    diamond.getCarat() != request.getCarat()) {
                    
                    responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                            .withDescription("Certified attributes (Cut, Clarity, Color, Carat) cannot be modified after a certificate is linked.")
                            .asRuntimeException());
                    return;
                }
            }

            diamond.setCut(request.getCut());
            diamond.setClarity(request.getClarity());
            diamond.setColor(request.getColor());
            diamond.setCarat(request.getCarat());
            // ownerId is not a certified attribute, so it can be updated
            diamond.setOwnerId(request.getOwnerId());
            
            Diamond saved = diamondRepository.save(diamond);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('admin')")
    public void verifyCertification(CertificateRequest request, StreamObserver<CertificateResponse> responseObserver) {
        boolean isValid = diamondRepository.findByCertificateId(request.getCertificateId()).isPresent();
        responseObserver.onNext(CertificateResponse.newBuilder()
                .setCertificateId(request.getCertificateId())
                .setIsValid(isValid)
                .setMessage(isValid ? "Valid Certificate" : "Invalid or Not Found")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAuthority('supplier')")
    public void linkCertificate(LinkCertificateRequest request, StreamObserver<DiamondResponse> responseObserver) {
        diamondRepository.findById(request.getDiamondId()).ifPresentOrElse(diamond -> {
            Long certId = request.getCertificateId();
            
            // Check if certificate ID is already in use by another diamond
            if (diamondRepository.findByCertificateId(certId).isPresent()) {
                 responseObserver.onError(io.grpc.Status.ALREADY_EXISTS
                        .withDescription("Certificate ID " + certId + " is already linked to another diamond")
                        .asRuntimeException());
                return;
            }

            diamond.setCertificateId(certId);
            diamond.setStatus("CERTIFIED");
            Diamond saved = diamondRepository.save(diamond);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyAuthority('supplier', 'shipper', 'vault_manager', 'insurance_agent')")
    public void getDiamondById(DiamondIdRequest request, StreamObserver<DiamondResponse> responseObserver) {
        diamondRepository.findById(request.getId()).ifPresentOrElse(diamond -> {
            responseObserver.onNext(mapToResponse(diamond));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyAuthority('supplier', 'shipper')")
    public void searchDiamonds(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        List<Diamond> diamonds = diamondRepository.findByCutContainingOrClarityContainingOrColorContaining(
                request.getQuery(), request.getQuery(), request.getQuery());
        
        SearchResponse response = SearchResponse.newBuilder()
                .addAllDiamonds(diamonds.stream().map(this::mapToResponse).collect(Collectors.toList()))
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private DiamondResponse mapToResponse(Diamond diamond) {
        return DiamondResponse.newBuilder()
                .setId(diamond.getId() != null ? diamond.getId() : 0L)
                .setCut(diamond.getCut() != null ? diamond.getCut() : "")
                .setClarity(diamond.getClarity() != null ? diamond.getClarity() : "")
                .setColor(diamond.getColor() != null ? diamond.getColor() : "")
                .setCarat(diamond.getCarat())
                .setCertificateId(diamond.getCertificateId() != null ? diamond.getCertificateId() : 0L)
                .setOwnerId(diamond.getOwnerId() != null ? diamond.getOwnerId() : 0L)
                .setStatus(diamond.getStatus() != null ? diamond.getStatus() : "")
                .build();
    }
}
