package com.aegisdiamond.payment.service;

import com.aegisdiamond.payment.entity.Transaction;
import com.aegisdiamond.payment.grpc.*;
import com.aegisdiamond.payment.repository.TransactionRepository;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentGrpcServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudCheckService fraudCheckService;

    @Mock
    private StreamObserver<PaymentResponse> paymentObserver;

    private PaymentGrpcService paymentGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        paymentGrpcService = new PaymentGrpcService();
        setPrivateField(paymentGrpcService, "transactionRepository", transactionRepository);
        setPrivateField(paymentGrpcService, "fraudCheckService", fraudCheckService);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void initiatePayment_SmallAmount() {
        Transaction saved = new Transaction();
        saved.setId(1L);
        saved.setStatus("INITIATED");

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        PaymentRequest request = PaymentRequest.newBuilder()
                .setShipmentId(100L)
                .setAmount(50000.0)
                .setCurrency("USD")
                .setPayerId(1L)
                .setPayeeId(2L)
                .setUseEscrow(false)
                .build();

        paymentGrpcService.initiatePayment(request, paymentObserver);

        ArgumentCaptor<PaymentResponse> captor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentObserver).onNext(captor.capture());

        PaymentResponse response = captor.getValue();
        assertEquals("INITIATED", response.getStatus());
        assertFalse(response.getAmount() > 100000.0);
    }

    @Test
    void initiatePayment_LargeAmount_UsesEscrow() {
        Transaction saved = new Transaction();
        saved.setId(1L);
        saved.setUseEscrow(true);
        saved.setStatus("PENDING_ESCROW");

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        PaymentRequest request = PaymentRequest.newBuilder()
                .setShipmentId(100L)
                .setAmount(200000.0)
                .setCurrency("USD")
                .setPayerId(1L)
                .setPayeeId(2L)
                .build();

        paymentGrpcService.initiatePayment(request, paymentObserver);

        ArgumentCaptor<PaymentResponse> captor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentObserver).onNext(captor.capture());

        assertEquals("PENDING_ESCROW", captor.getValue().getStatus());
    }

    @Test
    void confirmPayment_Success() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setStatus("INITIATED");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        ConfirmRequest request = ConfirmRequest.newBuilder()
                .setTransactionId(1L)
                .build();

        paymentGrpcService.confirmPayment(request, paymentObserver);

        ArgumentCaptor<PaymentResponse> captor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentObserver).onNext(captor.capture());

        assertEquals("CONFIRMED", captor.getValue().getStatus());
    }

    @Test
    void confirmPayment_AlreadyConfirmed() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setStatus("CONFIRMED");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        ConfirmRequest request = ConfirmRequest.newBuilder()
                .setTransactionId(1L)
                .build();

        paymentGrpcService.confirmPayment(request, paymentObserver);
        verify(paymentObserver).onError(any(Throwable.class));
    }

    @Test
    void processEscrow_Success() {
        Transaction saved = new Transaction();
        saved.setId(1L);
        saved.setStatus("PENDING_ESCROW");

        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        PaymentRequest request = PaymentRequest.newBuilder()
                .setShipmentId(100L)
                .setAmount(150000.0)
                .setCurrency("USD")
                .setPayerId(1L)
                .setPayeeId(2L)
                .build();

        paymentGrpcService.processEscrow(request, paymentObserver);

        verify(paymentObserver).onNext(any(PaymentResponse.class));
        verify(paymentObserver).onCompleted();
    }

    @Test
    void releaseFunds_Success() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setUseEscrow(true);
        transaction.setAmount(100000.0);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(fraudCheckService.isTransactionSafe(anyLong(), anyDouble())).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        ConfirmRequest request = ConfirmRequest.newBuilder()
                .setTransactionId(1L)
                .build();

        paymentGrpcService.releaseFunds(request, paymentObserver);

        ArgumentCaptor<PaymentResponse> captor = ArgumentCaptor.forClass(PaymentResponse.class);
        verify(paymentObserver).onNext(captor.capture());

        assertEquals("RELEASED", captor.getValue().getStatus());
    }

    @Test
    void releaseFunds_FraudCheckFails() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setUseEscrow(true);
        transaction.setAmount(100000.0);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(fraudCheckService.isTransactionSafe(anyLong(), anyDouble())).thenReturn(false);

        ConfirmRequest request = ConfirmRequest.newBuilder()
                .setTransactionId(1L)
                .build();

        paymentGrpcService.releaseFunds(request, paymentObserver);
        verify(paymentObserver).onError(any(Throwable.class));
        assertEquals("FAILED", transaction.getStatus());
    }

    @Test
    void releaseFunds_NotEscrowTransaction() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setUseEscrow(false);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        ConfirmRequest request = ConfirmRequest.newBuilder()
                .setTransactionId(1L)
                .build();

        paymentGrpcService.releaseFunds(request, paymentObserver);
        verify(paymentObserver).onError(any(Throwable.class));
    }

    @Test
    void getTransactionDetails_Success() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setStatus("CONFIRMED");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        TransactionIdRequest request = TransactionIdRequest.newBuilder()
                .setId(1L)
                .build();

        paymentGrpcService.getTransactionDetails(request, paymentObserver);

        verify(paymentObserver).onNext(any(PaymentResponse.class));
        verify(paymentObserver).onCompleted();
    }
}
