package com.orbit.ecommerce.notification.service;

import com.orbit.ecommerce.notification.dto.NotificationRequest;
import com.orbit.ecommerce.notification.dto.NotificationResponse;
import com.orbit.ecommerce.notification.exception.ResourceNotFoundException;
import com.orbit.ecommerce.notification.model.Notification;
import com.orbit.ecommerce.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse create(NotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .message(request.getMessage())
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        // MOCK dispatch: replace with real email/SMS/push or an event publish
        // (e.g. to a message broker) once the notification channel is decided.
        log.info("Dispatching notification [{}] to user {}: {}", saved.getType(), saved.getUserId(), saved.getMessage());

        return toResponse(saved);
    }

    public NotificationResponse getById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return toResponse(notification);
    }

    public List<NotificationResponse> getByUserId(Long userId) {
        return notificationRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
