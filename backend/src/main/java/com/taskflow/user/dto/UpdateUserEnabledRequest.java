package com.taskflow.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserEnabledRequest(@NotNull Boolean enabled) {
}
