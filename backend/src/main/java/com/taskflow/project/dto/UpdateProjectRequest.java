package com.taskflow.project.dto;

import com.taskflow.project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate
) {
}
