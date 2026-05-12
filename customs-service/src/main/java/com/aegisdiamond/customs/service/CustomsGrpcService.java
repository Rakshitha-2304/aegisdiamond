package com.aegisdiamond.customs.service;

import com.aegisdiamond.customs.entity.CustomsDeclaration;
import com.aegisdiamond.customs.grpc.*;
import com.aegisdiamond.customs.repository.CustomsRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;

@GrpcService
public class CustomsGrpcService extends CustomsServiceGrpc.CustomsServiceImplBase {

    @Autowired
    private CustomsRepository customsRepository;

    @Autowired
    private ComplianceEngine complianceEngine;

    @Override
    @PreAuthorize("hasAuthority('customs_officer')")
    public void validateCustomsDocuments(CustomsRequest request, StreamObserver<CustomsResponse> responseObserver) {
        boolean isValid = complianceEngine.validateDocuments(request.getOriginCountry(), request.getDestinationCountry(), request.getDocumentIdsList());
        
        responseObserver.onNext(CustomsResponse.newBuilder()
                .setShipmentId(request.getShipmentId())
                .setIsCompliant(isValid)
                .setStatus(isValid ? "DOCUMENTS_VALIDATED" : "INVALID_DOCUMENTS")
                .setMessage(isValid ? "Compliance requirements met." : "Insufficient documentation for route. Required: " + complianceEngine.getComplianceRequirement(request.getDestinationCountry()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAnyAuthority('shipper', 'customs_officer')")
    public void submitCustomsDeclaration(CustomsRequest request, StreamObserver<CustomsResponse> responseObserver) {
        CustomsDeclaration declaration = customsRepository.findByShipmentId(request.getShipmentId())
                .orElse(new CustomsDeclaration());
        
        declaration.setShipmentId(request.getShipmentId());
        declaration.setOriginCountry(request.getOriginCountry());
        declaration.setDestinationCountry(request.getDestinationCountry());
        declaration.setDeclarationValue(request.getDeclarationValue());
        declaration.setDocumentIds(request.getDocumentIdsList());
        declaration.setCompliant(complianceEngine.validateDocuments(request.getOriginCountry(), request.getDestinationCountry(), request.getDocumentIdsList()));
        declaration.setStatus("SUBMITTED");
        
        CustomsDeclaration saved = customsRepository.save(declaration);
        
        responseObserver.onNext(mapToResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAuthority('customs_officer')")
    public void approveCustomsClearance(CustomsIdRequest request, StreamObserver<CustomsResponse> responseObserver) {
        customsRepository.findByShipmentId(request.getShipmentId()).ifPresentOrElse(declaration -> {
            if (!declaration.isCompliant()) {
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription("Cannot approve non-compliant declaration").asRuntimeException());
                return;
            }
            declaration.setStatus("APPROVED");
            declaration.setClearedAt(LocalDateTime.now());
            CustomsDeclaration saved = customsRepository.save(declaration);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Declaration not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyAuthority('shipper', 'customs_officer')")
    public void getComplianceStatus(CustomsIdRequest request, StreamObserver<CustomsResponse> responseObserver) {
        customsRepository.findByShipmentId(request.getShipmentId()).ifPresentOrElse(declaration -> {
            responseObserver.onNext(mapToResponse(declaration));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Declaration not found").asRuntimeException());
        });
    }

    private CustomsResponse mapToResponse(CustomsDeclaration declaration) {
        return CustomsResponse.newBuilder()
                .setShipmentId(declaration.getShipmentId() != null ? declaration.getShipmentId() : 0L)
                .setStatus(declaration.getStatus())
                .setIsCompliant(declaration.isCompliant())
                .setMessage("Status: " + declaration.getStatus() + ". Compliant: " + declaration.isCompliant())
                .build();
    }
}
