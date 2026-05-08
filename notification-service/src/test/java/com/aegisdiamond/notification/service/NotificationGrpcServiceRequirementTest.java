package com.aegisdiamond.notification.service;

import com.aegisdiamond.notification.entity.Notification;
import com.aegisdiamond.notification.grpc.*;
import com.aegisdiamond.notification.repository.NotificationRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationGrpcServiceRequirementTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AlertDispatcher alertDispatcher;

    @Mock
    private StreamObserver<NotificationResponse> responseObserver;

    private NotificationGrpcService notificationGrpcService;

    @BeforeEach
    void setUp() throws Exception {
        notificationGrpcService = new NotificationGrpcService();
        Field f1 = notificationGrpcService.getClass().getDeclaredField("notificationRepository");
        f1.setAccessible(true);
        f1.set(notificationGrpcService, notificationRepository);
        Field f2 = notificationGrpcService.getClass().getDeclaredField("alertDispatcher");
        f2.setAccessible(true);
        f2.set(notificationGrpcService, alertDispatcher);
    }

    @Test
    @DisplayName("Requirement: Real-time alerts for security events")
    void sendSecurityAlert_ShouldDispatchAlert() {
        // Arrange
        AlertRequest request = AlertRequest.newBuilder()
                .setShipmentId(1L)
                .setMessage("Unscheduled stop detected")
                .setSeverity("CRITICAL")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(new Notification());

        // Act
        notificationGrpcService.sendSecurityAlert(request, responseObserver);

        // Assert
        verify(alertDispatcher).dispatch(eq("Security Alert"), anyString(), eq("CRITICAL"));
        verify(notificationRepository).save(any(Notification.class));
    }
}
