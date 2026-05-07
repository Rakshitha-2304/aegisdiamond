package com.aegisdiamond.diamond.service;

import com.aegisdiamond.diamond.entity.Diamond;
import com.aegisdiamond.diamond.grpc.*;
import com.aegisdiamond.diamond.repository.DiamondRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class DiamondGrpcService extends DiamondServiceGrpc.DiamondServiceImplBase {

    @Autowired
    private DiamondRepository diamondRepository;

    @Override
    public void registerDiamond(DiamondRequest request, StreamObserver<DiamondResponse> responseObserver) {
        Diamond diamond = new Diamond();
        diamond.setCut(request.getCut());
        diamond.setClarity(request.getClarity());
        diamond.setColor(request.getColor());
        diamond.setCarat(request.getCarat());
        diamond.setCertificateId(request.getCertificateId());
        diamond.setOwnerId(request.getOwnerId());
        diamond.setStatus("REGISTERED");

        Diamond saved = diamondRepository.save(diamond);
        responseObserver.onNext(mapToResponse(saved));
        responseObserver.onCompleted();
    }

    @Override
    public void updateDiamondDetails(DiamondRequest request, StreamObserver<DiamondResponse> responseObserver) {
        diamondRepository.findById(request.getId()).ifPresentOrElse(diamond -> {
            diamond.setCut(request.getCut());
            diamond.setClarity(request.getClarity());
            diamond.setColor(request.getColor());
            diamond.setCarat(request.getCarat());
            Diamond saved = diamondRepository.save(diamond);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found").asRuntimeException());
        });
    }

    @Override
    public void verifyCertification(CertificateRequest request, StreamObserver<CertificateResponse> responseObserver) {
        boolean isValid = diamondRepository.findByCertificateId(request.getCertificateId()).isPresent();
        responseObserver.onNext(CertificateResponse.newBuilder()
                .setIsValid(isValid)
                .setDetails(isValid ? "Valid Certificate" : "Invalid or Not Found")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void linkCertificate(LinkCertificateRequest request, StreamObserver<DiamondResponse> responseObserver) {
        diamondRepository.findById(request.getDiamondId()).ifPresentOrElse(diamond -> {
            diamond.setCertificateId(request.getCertificateId());
            diamond.setStatus("CERTIFIED");
            Diamond saved = diamondRepository.save(diamond);
            responseObserver.onNext(mapToResponse(saved));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found").asRuntimeException());
        });
    }

    @Override
    public void getDiamondById(DiamondIdRequest request, StreamObserver<DiamondResponse> responseObserver) {
        diamondRepository.findById(request.getId()).ifPresentOrElse(diamond -> {
            responseObserver.onNext(mapToResponse(diamond));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Diamond not found").asRuntimeException());
        });
    }

    @Override
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
                .setId(diamond.getId())
                .setCut(diamond.getCut())
                .setClarity(diamond.getClarity())
                .setColor(diamond.getColor())
                .setCarat(diamond.getCarat())
                .setCertificateId(diamond.getCertificateId() != null ? diamond.getCertificateId() : "")
                .setStatus(diamond.getStatus())
                .build();
    }
}
