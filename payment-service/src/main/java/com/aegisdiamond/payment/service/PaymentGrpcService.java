package com.aegisdiamond.payment.service;

import com.aegisdiamond.payment.entity.Transaction;
import com.aegisdiamond.payment.grpc.*;
import com.aegisdiamond.payment.repository.TransactionRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudCheckService fraudCheckService;

    private static final double ESCROW_THRESHOLD = 100000.0;

    @Override
    @PreAuthorize("hasRole('SUPPLIER')")
    public void initiatePayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        Transaction transaction = new Transaction();
        transaction.setShipmentId(request.getShipmentId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPayerId(request.getPayerId());
        transaction.setPayeeId(request.getPayeeId());
        
        boolean useEscrow = request.getUseEscrow() || request.getAmount() >= ESCROW_THRESHOLD;
        transaction.setUseEscrow(useEscrow);
        transaction.setStatus(useEscrow ? "PENDING_ESCROW" : "INITIATED");

        Transaction saved = transactionRepository.save(transaction);
        responseObserver.onNext(mapToResponse(saved, "Payment initiated."));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('SHIPPER')")
    public void confirmPayment(ConfirmRequest request, StreamObserver<PaymentResponse> responseObserver) {
        transactionRepository.findById(request.getTransactionId()).ifPresentOrElse(transaction -> {
            if (transaction.getStatus().equals("CONFIRMED") || transaction.getStatus().equals("RELEASED")) {
                responseObserver.onError(io.grpc.Status.ALREADY_EXISTS.withDescription("Transaction already processed").asRuntimeException());
                return;
            }

            transaction.setStatus("CONFIRMED");
            Transaction saved = transactionRepository.save(transaction);
            responseObserver.onNext(mapToResponse(saved, "Payment confirmed."));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Transaction not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void processEscrow(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        Transaction transaction = new Transaction();
        transaction.setShipmentId(request.getShipmentId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPayerId(request.getPayerId());
        transaction.setPayeeId(request.getPayeeId());
        transaction.setUseEscrow(true);
        transaction.setStatus("PENDING_ESCROW");

        Transaction saved = transactionRepository.save(transaction);
        responseObserver.onNext(mapToResponse(saved, "Escrow processing initiated."));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void releaseFunds(ConfirmRequest request, StreamObserver<PaymentResponse> responseObserver) {
        transactionRepository.findById(request.getTransactionId()).ifPresentOrElse(transaction -> {
            if (!transaction.isUseEscrow()) {
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription("Not an escrow transaction").asRuntimeException());
                return;
            }

            if (!fraudCheckService.isTransactionSafe(transaction.getId() != null ? transaction.getId() : 0L, transaction.getAmount())) {
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                responseObserver.onError(io.grpc.Status.PERMISSION_DENIED.withDescription("Fraud check failed for release").asRuntimeException());
                return;
            }

            transaction.setStatus("RELEASED");
            Transaction saved = transactionRepository.save(transaction);
            responseObserver.onNext(mapToResponse(saved, "Funds released from escrow."));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Transaction not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPPLIER', 'SHIPPER')")
    public void getTransactionDetails(TransactionIdRequest request, StreamObserver<PaymentResponse> responseObserver) {
        transactionRepository.findById(request.getId()).ifPresentOrElse(transaction -> {
            responseObserver.onNext(mapToResponse(transaction, "Details retrieved."));
            responseObserver.onCompleted();
        }, () -> {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Transaction not found").asRuntimeException());
        });
    }

    private PaymentResponse mapToResponse(Transaction transaction, String message) {
        return PaymentResponse.newBuilder()
                .setTransactionId(transaction.getId() != null ? transaction.getId() : 0L)
                .setStatus(transaction.getStatus())
                .setAmount(transaction.getAmount())
                .setMessage(message)
                .build();
    }
}
