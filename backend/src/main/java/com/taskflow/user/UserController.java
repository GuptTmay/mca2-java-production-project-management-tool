package com.taskflow.user;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.user.dto.UpdateProfileRequest;
import com.taskflow.user.dto.UpdateRoleRequest;
import com.taskflow.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return userService.getById(id, currentUser);
    }

    @PatchMapping("/{id}")
    public UserResponse updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User currentUser) {
        return userService.updateProfile(id, request, currentUser);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(id, request);
    }
}
