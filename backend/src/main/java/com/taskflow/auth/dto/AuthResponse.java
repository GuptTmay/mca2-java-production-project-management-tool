package com.taskflow.auth.dto;

import com.taskflow.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
