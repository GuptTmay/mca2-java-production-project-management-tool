package com.taskflow.auth;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RefreshRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.user.User;
import com.taskflow.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Stateless JWT: there is no server-side session or token store to clear.
        // The client is responsible for discarding the access/refresh tokens.
        // (A token-denylist would be the next step if server-side revocation
        // becomes a requirement - out of scope for the Core feature set.)
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return authService.getCurrentUser(user);
    }
}
