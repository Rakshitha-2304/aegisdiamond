package com.aegisdiamond.notification.service;

import com.aegisdiamond.notification.entity.Notification;
import com.aegisdiamond.notification.grpc.*;
import com.aegisdiamond.notification.repository.NotificationRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationGrpcServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AlertDispatcher alertDispatcher;

    @Mock
    private StreamObserver<NotificationResponse> notificationObserver;

    @Mock
    private StreamObserver<NotificationListResponse> listObserver;

    private NotificationGrpcService notificationGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        notificationGrpcService = new NotificationGrpcService();
        setPrivateField(notificationGrpcService, "notificationRepository", notificationRepository);
        setPrivateField(notificationGrpcService, "alertDispatcher", alertDispatcher);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void sendSecurityAlert_Success() {
        Notification saved = new Notification();
        saved.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        AlertRequest request = AlertRequest.newBuilder()
                .setShipmentId(100L)
                .setMessage("Security breach detected")
                .setSeverity("CRITICAL")
                .build();

        notificationGrpcService.sendSecurityAlert(request, notificationObserver);

        ArgumentCaptor<NotificationResponse> captor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(notificationObserver).onNext(captor.capture());
        verify(alertDispatcher).dispatch(eq("Security Alert"), anyString(), eq("CRITICAL"));

        assertEquals("SENT", captor.getValue().getStatus());
    }

    @Test
    void sendShipmentUpdate_Success() {
        Notification saved = new Notification();
        saved.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        UpdateRequest request = UpdateRequest.newBuilder()
                .setShipmentId(100L)
                .setMessage("Shipment has been delivered")
                .build();

        notificationGrpcService.sendShipmentUpdate(request, notificationObserver);

        verify(notificationObserver).onNext(any(NotificationResponse.class));
        verify(alertDispatcher).dispatch(eq("Shipment Update"), anyString(), eq("INFO"));
    }

    @Test
    void sendRiskAlert_Success() {
        Notification saved = new Notification();
        saved.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        AlertRequest request = AlertRequest.newBuilder()
                .setShipmentId(100L)
                .setMessage("High risk score detected")
                .setSeverity("HIGH")
                .build();

        notificationGrpcService.sendRiskAlert(request, notificationObserver);

        verify(notificationObserver).onNext(any(NotificationResponse.class));
        verify(alertDispatcher).dispatch(eq("Risk Alert"), anyString(), eq("HIGH"));
    }

    @Test
    void getNotifications_ReturnsList() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setShipmentId(100L);
        notification.setType("SECURITY_ALERT");

        when(notificationRepository.findAll()).thenReturn(List.of(notification));

        UserRequest request = UserRequest.newBuilder().setUserId(1L).build();

        notificationGrpcService.getNotifications(request, listObserver);

        verify(listObserver).onNext(any(NotificationListResponse.class));
        verify(listObserver).onCompleted();
    }
}
