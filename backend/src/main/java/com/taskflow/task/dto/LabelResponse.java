package com.taskflow.task.dto;

import java.util.UUID;

public record LabelResponse(
        UUID id,
        String name,
        String color
) {
}
