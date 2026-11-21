package com.taskflow.project.dto;

import com.taskflow.project.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID teamId,
        String name,
        String code,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
