package com.aegisdiamond.payment.service;

import com.aegisdiamond.payment.entity.Transaction;
import com.aegisdiamond.payment.grpc.*;
import com.aegisdiamond.payment.repository.TransactionRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGrpcService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudCheckService fraudCheckService;

    private static final double ESCROW_THRESHOLD = 100000.0;

    @Override
    @PreAuthorize("hasAuthority('supplier')")
    public void initiatePayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        logger.info("Initiating payment for shipment ID {}: amount={} {}", request.getShipmentId(), request.getAmount(), request.getCurrency());
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
        logger.info("Payment initiated successfully with transaction ID: {}. Escrow: {}", saved.getId(), useEscrow);
        responseObserver.onNext(mapToResponse(saved, "Payment initiated."));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAuthority('shipper')")
    public void confirmPayment(ConfirmRequest request, StreamObserver<PaymentResponse> responseObserver) {
        logger.info("Confirming payment for transaction ID: {}", request.getTransactionId());
        transactionRepository.findById(request.getTransactionId()).ifPresentOrElse(transaction -> {
            if (transaction.getStatus().equals("CONFIRMED") || transaction.getStatus().equals("RELEASED")) {
                logger.warn("Confirmation failed: Transaction ID {} already processed", request.getTransactionId());
                responseObserver.onError(io.grpc.Status.ALREADY_EXISTS.withDescription("Transaction already processed").asRuntimeException());
                return;
            }

            transaction.setStatus("CONFIRMED");
            Transaction saved = transactionRepository.save(transaction);
            logger.info("Payment for transaction ID {} confirmed successfully", saved.getId());
            responseObserver.onNext(mapToResponse(saved, "Payment confirmed."));
            responseObserver.onCompleted();
        }, () -> {
            logger.warn("Confirmation failed: Transaction ID {} not found", request.getTransactionId());
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Transaction not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAuthority('admin')")
    public void processEscrow(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        logger.info("Processing escrow for shipment ID {}: amount={}", request.getShipmentId(), request.getAmount());
        Transaction transaction = new Transaction();
        transaction.setShipmentId(request.getShipmentId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPayerId(request.getPayerId());
        transaction.setPayeeId(request.getPayeeId());
        transaction.setUseEscrow(true);
        transaction.setStatus("PENDING_ESCROW");

        Transaction saved = transactionRepository.save(transaction);
        logger.info("Escrow initiated successfully with transaction ID: {}", saved.getId());
        responseObserver.onNext(mapToResponse(saved, "Escrow processing initiated."));
        responseObserver.onCompleted();
    }

    @Override
    @PreAuthorize("hasAuthority('admin')")
    public void releaseFunds(ConfirmRequest request, StreamObserver<PaymentResponse> responseObserver) {
        logger.info("Releasing funds for transaction ID: {}", request.getTransactionId());
        transactionRepository.findById(request.getTransactionId()).ifPresentOrElse(transaction -> {
            if (!transaction.isUseEscrow()) {
                logger.warn("Release failed: Transaction ID {} is not an escrow transaction", request.getTransactionId());
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription("Not an escrow transaction").asRuntimeException());
                return;
            }

            if (!fraudCheckService.isTransactionSafe(transaction.getId() != null ? transaction.getId() : 0L, transaction.getAmount())) {
                logger.error("Release failed: Fraud check failed for transaction ID {}", transaction.getId());
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                responseObserver.onError(io.grpc.Status.PERMISSION_DENIED.withDescription("Fraud check failed for release").asRuntimeException());
                return;
            }

            transaction.setStatus("RELEASED");
            Transaction saved = transactionRepository.save(transaction);
            logger.info("Funds released successfully for transaction ID: {}", saved.getId());
            responseObserver.onNext(mapToResponse(saved, "Funds released from escrow."));
            responseObserver.onCompleted();
        }, () -> {
            logger.warn("Release failed: Transaction ID {} not found", request.getTransactionId());
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription("Transaction not found").asRuntimeException());
        });
    }

    @Override
    @PreAuthorize("hasAnyAuthority('supplier', 'shipper')")
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
