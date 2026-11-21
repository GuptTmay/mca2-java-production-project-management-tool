package com.taskflow.auth;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.security.JwtService;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserMapper;
import com.taskflow.user.UserRepository;
import com.taskflow.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, authenticationManager, userMapper);
    }

    @Test
    void register_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "taken@taskflow.io", "password123", "Ada", "Lovelace", Role.CONTENT_MANAGER);
        when(userRepository.existsByEmail("taken@taskflow.io")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesUserWithSelectedRoleAndEncodedPassword() {
        RegisterRequest request = new RegisterRequest(
                "new@taskflow.io", "password123", "Ada", "Lovelace", Role.PRODUCER);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(userMapper.toResponse(any())).thenReturn(new UserResponse(
                UUID.randomUUID(), "new@taskflow.io", "Ada", "Lovelace", Role.PRODUCER, true, Instant.now()));

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(Role.PRODUCER);
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getEmail()).isEqualTo("new@taskflow.io");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void register_normalizesEmailToLowercase() {
        RegisterRequest request = new RegisterRequest(
                "Mixed.Case@TaskFlow.io", "password123", "Ada", "Lovelace", Role.TEAM_MEMBER);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any())).thenReturn(new UserResponse(
                UUID.randomUUID(), "mixed.case@taskflow.io", "Ada", "Lovelace", Role.TEAM_MEMBER, true, Instant.now()));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("mixed.case@taskflow.io");
    }

    @Test
    void login_throwsBadCredentialsException_whenAuthenticationManagerRejects() {
        LoginRequest request = new LoginRequest("user@taskflow.io", "wrong-password");
        doThrow(new org.springframework.security.authentication.BadCredentialsException("bad creds"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_succeeds_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@taskflow.io", "correct-password");
        User user = new User("user@taskflow.io", "hashed", "Grace", "Hopper", Role.TEAM_MEMBER);
        when(userRepository.findByEmail("user@taskflow.io")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(userMapper.toResponse(user)).thenReturn(new UserResponse(
                UUID.randomUUID(), "user@taskflow.io", "Grace", "Hopper", Role.TEAM_MEMBER, true, Instant.now()));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().email()).isEqualTo("user@taskflow.io");
    }

    @Test
    void refresh_rejectsDisabledAccounts() {
        User user = new User("disabled@taskflow.io", "hashed", "Dee", "Activated", Role.TEAM_MEMBER);
        user.setEnabled(false);
        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("refresh-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> authService.refresh(new com.taskflow.auth.dto.RefreshRequest("refresh-token")));
        verify(jwtService, never()).generateAccessToken(any());
    }
}
