package com.taskflow.team;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAccessGuardTest {

    @Mock
    private TeamMemberRepository teamMemberRepository;

    private TeamAccessGuard guard;
    private final UUID teamId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new TeamAccessGuard(teamMemberRepository);
    }

    @Test
    void requireMembership_allowsAdminWithoutLookup() {
        User admin = user(Role.ADMIN);

        assertDoesNotThrow(() -> guard.requireMembership(teamId, admin));
        verify(teamMemberRepository, never()).existsByTeamIdAndUserId(any(), any());
    }

    @Test
    void requireMembership_allowsExistingMember() {
        User dev = user(Role.DEVELOPER);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, dev.getId())).thenReturn(true);

        assertDoesNotThrow(() -> guard.requireMembership(teamId, dev));
    }

    @Test
    void requireMembership_forbidsNonMember() {
        User dev = user(Role.DEVELOPER);
        when(teamMemberRepository.existsByTeamIdAndUserId(teamId, dev.getId())).thenReturn(false);

        assertThrows(ForbiddenOperationException.class, () -> guard.requireMembership(teamId, dev));
    }

    @Test
    void requireManageAccess_allowsOwnerAndManager() {
        User dev = user(Role.DEVELOPER);
        Team team = new Team("Platform", "desc", dev);
        TeamMember membership = new TeamMember(team, dev, TeamRole.MANAGER);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, dev.getId())).thenReturn(Optional.of(membership));

        assertDoesNotThrow(() -> guard.requireManageAccess(teamId, dev));
    }

    @Test
    void requireManageAccess_forbidsPlainMember() {
        User dev = user(Role.DEVELOPER);
        Team team = new Team("Platform", "desc", dev);
        TeamMember membership = new TeamMember(team, dev, TeamRole.MEMBER);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, dev.getId())).thenReturn(Optional.of(membership));

        assertThrows(ForbiddenOperationException.class, () -> guard.requireManageAccess(teamId, dev));
    }

    @Test
    void requireManageAccess_forbidsNonMemberEntirely() {
        User dev = user(Role.DEVELOPER);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, dev.getId())).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> guard.requireManageAccess(teamId, dev));
    }

    @Test
    void requireOwnerAccess_forbidsManager() {
        User dev = user(Role.DEVELOPER);
        Team team = new Team("Platform", "desc", dev);
        TeamMember membership = new TeamMember(team, dev, TeamRole.MANAGER);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, dev.getId())).thenReturn(Optional.of(membership));

        assertThrows(ForbiddenOperationException.class, () -> guard.requireOwnerAccess(teamId, dev));
    }

    @Test
    void requireOwnerAccess_allowsOwner() {
        User dev = user(Role.DEVELOPER);
        Team team = new Team("Platform", "desc", dev);
        TeamMember membership = new TeamMember(team, dev, TeamRole.OWNER);
        when(teamMemberRepository.findByTeamIdAndUserId(teamId, dev.getId())).thenReturn(Optional.of(membership));

        assertDoesNotThrow(() -> guard.requireOwnerAccess(teamId, dev));
    }

    @Test
    void requireOwnerAccess_allowsAdminWithoutLookup() {
        User admin = user(Role.ADMIN);

        assertDoesNotThrow(() -> guard.requireOwnerAccess(teamId, admin));
        verify(teamMemberRepository, never()).findByTeamIdAndUserId(any(), any());
    }

    private static User user(Role role) {
        User u = new User(role.name().toLowerCase() + "@taskflow.io", "hash", "First", "Last", role);
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return u;
    }
}
