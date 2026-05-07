package com.aegisdiamond.shipping.service;

import com.aegisdiamond.shipping.entity.Shipment;
import com.aegisdiamond.shipping.grpc.*;
import com.aegisdiamond.shipping.repository.ShipmentRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;

@GrpcService
public class ShippingGrpcService extends ShippingServiceGrpc.ShippingServiceImplBase {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Override
    public void createShipment(ShipmentRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        Shipment shipment = new Shipment();
        shipment.setOrigin(request.getOrigin());
        shipment.setDestination(request.getDestination());
        shipment.setDiamondIds(request.getDiamondIdsList());
        shipment.setShipperId(request.getShipperId());
        shipment.setStatus("CREATED");

        Shipment saved = shipmentRepository.save(shipment);
        responseObserver.onNext(mapToResponse(saved));
        responseObserver.onCompleted();
    }


    @Override
    public void updateShipmentDetails(ShipmentRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        shipmentRepository.findById(request.getId()).ifPresentOrElse(shipment -> {
            if (shipment.isSealed()) {
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION
                        .withDescription("Cannot update details of a sealed shipment")
                        .asRuntimeException());
                return;
            }
            shipment.setOrigin(request.getOrigin());
            shipment.setDestination(request.getDestination());
            shipment.setDiamondIds(request.getDiamondIdsList());
            Shipment saved = shipmentRepository.save(shipment);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    public void assignSecureContainer(ContainerRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        shipmentRepository.findById(request.getShipmentId()).ifPresentOrElse(shipment -> {
            shipment.setContainerId(request.getContainerId());
            shipment.setStatus("VERIFIED");
            Shipment saved = shipmentRepository.save(shipment);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    public void sealShipment(SealRequest request, StreamObserver<ShipmentResponse> responseObserver) {
        shipmentRepository.findById(request.getShipmentId()).ifPresentOrElse(shipment -> {
            shipment.setSealId(request.getSealId());
            shipment.setSealed(true);
            shipment.setStatus("SEALED");
            Shipment saved = shipmentRepository.save(shipment);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
    public void validateShipmentSecurity(SecurityRequest request, StreamObserver<SecurityResponse> responseObserver) {
        shipmentRepository.findById(request.getShipmentId()).ifPresentOrElse(shipment -> {
            boolean isValid = shipment.isSealed() && shipment.getContainerId() != null && !shipment.getContainerId().isEmpty();
            responseObserver.onNext(SecurityResponse.newBuilder()
                    .setIsValid(isValid)
                    .setSecurityReport(isValid ? "Security checks passed. Shipment is sealed and containerized." : "Security checks failed. Shipment not properly sealed or containerized.")
                    .build());
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Shipment not found").asRuntimeException());
        });
    }

    @Override
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
