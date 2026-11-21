package com.taskflow.notification;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    public PageResponse<NotificationResponse> listForUser(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page.map(notificationMapper::toResponse));
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("You can only mark your own notifications as read");
        }

        notification.setRead(true);
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, Pageable.unpaged()).getContent();
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public NotificationResponse create(UUID userId, NotificationType type, String message, UUID taskId) {
        Notification notification = new Notification(userId, type, message, taskId);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }
}
