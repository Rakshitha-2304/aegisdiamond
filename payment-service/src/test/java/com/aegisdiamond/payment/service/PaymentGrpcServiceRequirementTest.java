package com.aegisdiamond.payment.service;

import com.aegisdiamond.payment.entity.Transaction;
import com.aegisdiamond.payment.grpc.*;
import com.aegisdiamond.payment.repository.TransactionRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentGrpcServiceRequirementTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudCheckService fraudCheckService;

    @Mock
    private StreamObserver<PaymentResponse> responseObserver;

    private PaymentGrpcService paymentGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        paymentGrpcService = new PaymentGrpcService();
        Field f1 = paymentGrpcService.getClass().getDeclaredField("transactionRepository");
        f1.setAccessible(true);
        f1.set(paymentGrpcService, transactionRepository);
        Field f2 = paymentGrpcService.getClass().getDeclaredField("fraudCheckService");
        f2.setAccessible(true);
        f2.set(paymentGrpcService, fraudCheckService);
    }

    @Test
    @DisplayName("Requirement: Escrow for high-value transactions (>= 100k)")
    void initiatePayment_HighValue_ShouldUseEscrowAutomatically() {
        // Arrange
        PaymentRequest request = PaymentRequest.newBuilder()
                .setAmount(150000.0) // > 100,000
                .setShipmentId(1L)
                .build();

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        when(transactionRepository.save(captor.capture())).thenReturn(new Transaction());

        // Act
        paymentGrpcService.initiatePayment(request, responseObserver);

        // Assert
        assertTrue(captor.getValue().isUseEscrow());
        assertEquals("PENDING_ESCROW", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Requirement: Fraud checks before release")
    void releaseFunds_FraudDetected_ShouldFail() {
        // Arrange
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setAmount(150000.0);
        tx.setUseEscrow(true);
        
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(fraudCheckService.isTransactionSafe(1L, 150000.0)).thenReturn(false);

        ConfirmRequest request = ConfirmRequest.newBuilder().setTransactionId(1L).build();

        // Act & Assert
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            paymentGrpcService.releaseFunds(request, responseObserver);
        });

        assertEquals(Status.Code.PERMISSION_DENIED, exception.getStatus().getCode());
        verify(transactionRepository).save(argThat(t -> t.getStatus().equals("FAILED")));
    }
}
