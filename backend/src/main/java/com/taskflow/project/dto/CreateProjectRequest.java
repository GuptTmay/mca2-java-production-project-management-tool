package com.taskflow.project.dto;

import com.taskflow.project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateProjectRequest(
        @NotNull UUID teamId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 50) String code,
        @Size(max = 2000) String description,
        ProjectStatus status,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
