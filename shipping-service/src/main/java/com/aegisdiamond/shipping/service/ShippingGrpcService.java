package com.aegisdiamond.shipping.service;

import com.aegisdiamond.shipping.entity.Shipment;
import com.aegisdiamond.shipping.grpc.*;
import com.aegisdiamond.shipping.repository.ShipmentRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;

@GrpcService
public class ShippingGrpcService extends ShippingServiceGrpc.ShippingServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(ShippingGrpcService.class);

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Override
    @PreAuthorize("hasAnyAuthority('supplier', 'shipper')")
    public void createShipment(ShipmentRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        logger.info("Creating new shipment from {} to {}", request.getOrigin(), request.getDestination());
        Shipment shipment = new Shipment();
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setDiamondIds(request.getDiamondIdsList());
        shipment.setShipperId(request.getShipperId());
        shipment.setStatus("CREATED");

        Shipment saved = shipmentRepository.save(shipment);
        logger.info("Shipment created with ID: {}", saved.getId());
        responseObserver.onNext(mapToResponse(saved));
        responseObserver.onCompleted();
    }


    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void updateShipmentDetails(ShipmentRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        logger.info("Updating shipment details for ID: {}", request.getId());
        shipmentRepository.findById(request.getId()).ifPresentOrElse(shipment -> {
            if (shipment.isSealed()) {
                logger.warn("Update failed: Shipment ID {} is already sealed", request.getId());
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                        .withDescription("Cannot update details of a sealed shipment")
                        .asRuntimeException());
                return;
            }
            shipment.setOrigin(request.getOrigin());
            shipment.setDestination(request.getDestination());
            shipment.setDiamondIds(request.getDiamondIdsList());
            Shipment saved = shipmentRepository.save(shipment);
            logger.info("Shipment ID {} updated successfully", saved.getId());
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            logger.warn("Update failed: Shipment ID {} not found", request.getId());
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void assignSecureContainer(ContainerRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        logger.info("Assigning container {} to shipment ID {}", request.getContainerId(), request.getShipmentId());
        shipmentRepository.findById(request.getShipmentId()).ifPresentOrElse(shipment -> {
            shipment.setContainerId(request.getContainerId());
            shipment.setStatus("VERIFIED");
            Shipment saved = shipmentRepository.save(shipment);
            logger.info("Container {} assigned to shipment ID {}", request.getContainerId(), saved.getId());
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            logger.warn("Container assignment failed: Shipment ID {} not found", request.getShipmentId());
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void sealShipment(SealRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        logger.info("Sealing shipment ID {} with seal {}", request.getShipmentId(), request.getSealId());
        shipmentRepository.findById(request.getShipmentId()).ifPresentOrElse(shipment -> {
            shipment.setSealId(request.getSealId());
            shipment.setSealed(true);
            shipment.setStatus("SEALED");
            Shipment saved = shipmentRepository.save(shipment);
            logger.info("Shipment ID {} sealed successfully", saved.getId());
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            logger.warn("Sealing failed: Shipment ID {} not found", request.getShipmentId());
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void validateShipmentSecurity(SecurityRequest request, StreamObserver<SecurityResponse> responseObserver) {
        logger.info("Validating security for shipment ID {}", request.getShipmentId());
        shipmentRepository.findById(request.getShipmentId()).ifPresentOrElse(shipment -> {
            boolean isValid = shipment.isSealed() && shipment.getContainerId() != null && !shipment.getContainerId().isEmpty();
            logger.info("Security validation for shipment ID {}: {}", request.getShipmentId(), isValid ? "PASSED" : "FAILED");
            responseObserver.onNext(SecurityResponse.newBuilder()
                    .setIsValid(isValid)
                    .setSecurityReport(isValid ? "Security checks passed. Shipment is sealed and containerized." : "Security checks failed. Shipment not properly sealed or containerized.")
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            logger.warn("Security validation failed: Shipment ID {} not found", request.getShipmentId());
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyAuthority('supplier', 'shipper', 'insurance_agent', 'customs_officer')")
    public void getShipmentDetails(ShipmentIdRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        shipmentRepository.findById(request.getId()).ifPresentOrElse(shipment -> {
            responseObserver.onNext(mapToResponse(shipment));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    private ShipmentResponse mapToResponse(Shipment shipment) {
        return ShipmentResponse.newBuilder()
                .setId(shipment.getId() != null ? shipment.getId() : 0L)
                .setOrigin(shipment.getOrigin())
                .setDestination(shipment.getDestination())
                .addAllDiamondIds(shipment.getDiamondIds())
                .setStatus(shipment.getStatus())
                .setContainerId(shipment.getContainerId() != null ? shipment.getContainerId() : "")
                .setIsSealed(shipment.isSealed())
                .setShipperId(shipment.getShipperId() != null ? shipment.getShipperId() : 0L)
                .build();
    }
}
