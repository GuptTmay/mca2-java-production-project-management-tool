package com.taskflow.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID taskId,
        UUID userId,
        String userFirstName,
        String userLastName,
        String userEmail,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
