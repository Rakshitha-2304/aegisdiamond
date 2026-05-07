package com.aegisdiamond.notification.service;

import com.aegisdiamond.notification.entity.Notification;
import com.aegisdiamond.notification.grpc.*;
import com.aegisdiamond.notification.repository.NotificationRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AlertDispatcher alertDispatcher;

    @Override
    public void sendSecurityAlert(AlertRequest request, StreamObserver<NotificationResponse> responseObserver) {
        Notification notification = new Notification();
        notification.setShipmentId(request.getShipmentId());
        notification.setType("SECURITY_ALERT");
        notification.setMessage(request.getMessage());
        notification.setSeverity(request.getSeverity());

        Notification saved = notificationRepository.save(notification);
        alertDispatcher.dispatch("Security Alert", request.getMessage(), request.getSeverity());

        responseObserver.onNext(NotificationResponse.newBuilder()
                .setNotificationId(saved.getId() != null ? saved.getId() : 0L)
                .setStatus("SENT")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendShipmentUpdate(UpdateRequest request, StreamObserver<NotificationResponse> responseObserver) {
        Notification notification = new Notification();
        notification.setShipmentId(request.getShipmentId());
        notification.setType("SHIPMENT_UPDATE");
        notification.setMessage(request.getMessage());
        notification.setSeverity("INFO");

        Notification saved = notificationRepository.save(notification);
        alertDispatcher.dispatch("Shipment Update", request.getMessage(), "INFO");

        responseObserver.onNext(NotificationResponse.newBuilder()
                .setNotificationId(saved.getId() != null ? saved.getId() : 0L)
                .setStatus("SENT")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendRiskAlert(AlertRequest request, StreamObserver<NotificationResponse> responseObserver) {
        Notification notification = new Notification();
        notification.setShipmentId(request.getShipmentId());
        notification.setType("RISK_ALERT");
        notification.setMessage(request.getMessage());
        notification.setSeverity(request.getSeverity());

        Notification saved = notificationRepository.save(notification);
        alertDispatcher.dispatch("Risk Alert", request.getMessage(), request.getSeverity());

        responseObserver.onNext(NotificationResponse.newBuilder()
                .setNotificationId(saved.getId() != null ? saved.getId() : 0L)
                .setStatus("SENT")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getNotifications(UserRequest request, StreamObserver<NotificationListResponse> responseObserver) {
        // Simplified: return all notifications for demonstration
        List<Notification> notifications = notificationRepository.findAll();
        
        NotificationListResponse response = NotificationListResponse.newBuilder()
                .addAllNotifications(notifications.stream()
                        .map(this::mapToDetail)
                        .collect(Collectors.toList()))
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private NotificationDetail mapToDetail(Notification n) {
        return NotificationDetail.newBuilder()
                .setId(n.getId() != null ? n.getId() : 0L)
                .setShipmentId(n.getShipmentId() != null ? n.getShipmentId() : 0L)
                .setType(n.getType())
                .setMessage(n.getMessage())
                .setTimestamp(n.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
