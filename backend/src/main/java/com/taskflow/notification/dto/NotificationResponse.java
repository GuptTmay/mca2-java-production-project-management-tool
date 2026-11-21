package com.taskflow.notification.dto;

import com.taskflow.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String message,
        UUID taskId,
        boolean read,
        Instant createdAt
) {
}
