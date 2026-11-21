package com.taskflow.user;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.user.dto.UpdateProfileRequest;
import com.taskflow.user.dto.UpdateRoleRequest;
import com.taskflow.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper);
    }

    @Test
    void getById_allowsUserToViewOwnProfile() {
        User self = withId(new User("me@taskflow.io", "hash", "Me", "Self", Role.DEVELOPER));
        when(userRepository.findById(self.getId())).thenReturn(Optional.of(self));
        when(userMapper.toResponse(self)).thenReturn(toResponse(self));

        UserResponse response = userService.getById(self.getId(), self);

        assertThat(response.email()).isEqualTo("me@taskflow.io");
    }

    @Test
    void getById_forbidsViewingSomeoneElsesProfile() {
        User self = withId(new User("me@taskflow.io", "hash", "Me", "Self", Role.DEVELOPER));
        User other = withId(new User("other@taskflow.io", "hash", "Other", "Person", Role.DEVELOPER));

        assertThrows(ForbiddenOperationException.class, () -> userService.getById(other.getId(), self));
    }

    @Test
    void getById_allowsAdminToViewAnyProfile() {
        User admin = withId(new User("admin@taskflow.io", "hash", "Admin", "User", Role.ADMIN));
        User other = withId(new User("other@taskflow.io", "hash", "Other", "Person", Role.DEVELOPER));
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(userMapper.toResponse(other)).thenReturn(toResponse(other));

        UserResponse response = userService.getById(other.getId(), admin);

        assertThat(response.email()).isEqualTo("other@taskflow.io");
    }

    @Test
    void getById_throwsNotFound_whenTargetUserMissing() {
        User admin = withId(new User("admin@taskflow.io", "hash", "Admin", "User", Role.ADMIN));
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getById(missingId, admin));
    }

    @Test
    void updateProfile_forbidsUpdatingSomeoneElsesProfile() {
        User self = withId(new User("me@taskflow.io", "hash", "Me", "Self", Role.DEVELOPER));
        User other = withId(new User("other@taskflow.io", "hash", "Other", "Person", Role.DEVELOPER));
        UpdateProfileRequest request = new UpdateProfileRequest("New", "Name");

        assertThrows(ForbiddenOperationException.class,
                () -> userService.updateProfile(other.getId(), request, self));
    }

    @Test
    void updateProfile_updatesOwnFirstAndLastName() {
        User self = withId(new User("me@taskflow.io", "hash", "Old", "Name", Role.DEVELOPER));
        UpdateProfileRequest request = new UpdateProfileRequest("New", "Name");
        when(userRepository.findById(self.getId())).thenReturn(Optional.of(self));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any())).thenAnswer(inv -> toResponse(inv.getArgument(0)));

        UserResponse response = userService.updateProfile(self.getId(), request, self);

        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.lastName()).isEqualTo("Name");
    }

    @Test
    void updateRole_changesRoleWithoutSelfAccessRestriction() {
        User target = withId(new User("dev@taskflow.io", "hash", "Dev", "User", Role.DEVELOPER));
        UpdateRoleRequest request = new UpdateRoleRequest(Role.PROJECT_MANAGER);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any())).thenAnswer(inv -> toResponse(inv.getArgument(0)));

        UserResponse response = userService.updateRole(target.getId(), request);

        assertThat(response.role()).isEqualTo(Role.PROJECT_MANAGER);
    }

    // --- Test helpers ---
    // User#id has no public setter (it's DB/JPA-generated), so tests assign it via
    // reflection to build fully-formed entities without needing a persistence context.

    private static User withId(User user) {
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.isEnabled(), Instant.now());
    }
}
