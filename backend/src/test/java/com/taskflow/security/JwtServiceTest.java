package com.taskflow.security;

import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final long ACCESS_EXP_MS = 15 * 60 * 1000;
    private static final long REFRESH_EXP_MS = 7L * 24 * 60 * 60 * 1000;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-only-secret-key-at-least-32-bytes-long!!",
                ACCESS_EXP_MS,
                REFRESH_EXP_MS);
    }

    @Test
    void generatesAccessTokenWithCorrectSubjectAndValidity() {
        User user = buildUser();

        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    void generatesRefreshTokenMarkedAsRefreshType() {
        User user = buildUser();

        String token = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
    }

    @Test
    void tokenIsInvalidForADifferentUser() {
        User original = buildUser();
        User impersonator = new User(
                "someone-else@taskflow.io", "hash", "Someone", "Else", Role.DEVELOPER);

        String token = jwtService.generateAccessToken(original);

        assertThat(jwtService.isTokenValid(token, impersonator)).isFalse();
    }

    @Test
    void malformedTokenIsRejectedGracefully() {
        User user = buildUser();

        assertThat(jwtService.isTokenValid("not-a-real-jwt", user)).isFalse();
        assertThat(jwtService.isRefreshToken("not-a-real-jwt")).isFalse();
    }

    @Test
    void constructorRejectsSecretShorterThan32Bytes() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new JwtService("too-short", ACCESS_EXP_MS, REFRESH_EXP_MS));
    }

    private User buildUser() {
        return new User("dev@taskflow.io", "hashed-password", "Dev", "User", Role.DEVELOPER);
    }
}
