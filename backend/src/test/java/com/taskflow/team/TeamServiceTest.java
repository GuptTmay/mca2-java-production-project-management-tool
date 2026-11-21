package com.taskflow.team;

import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.team.dto.AddTeamMemberRequest;
import com.taskflow.team.dto.CreateTeamRequest;
import com.taskflow.team.dto.TeamMemberResponse;
import com.taskflow.team.dto.TeamResponse;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Authorization pass/fail behavior itself (admin bypass, owner/manager/member
 * distinctions) is covered exhaustively in TeamAccessGuardTest. These tests
 * mock TeamAccessGuard and focus on what TeamService does once access is
 * granted or denied: business rules (duplicate members, last-owner
 * protection) and correct delegation to the guard.
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeamMapper teamMapper;
    @Mock
    private TeamMemberMapper teamMemberMapper;
    @Mock
    private TeamAccessGuard teamAccessGuard;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(
                teamRepository, teamMemberRepository, userRepository, teamMapper, teamMemberMapper, teamAccessGuard);
    }

    @Test
    void createTeam_savesCreatorAsOwner() {
        User creator = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team savedTeam = withTeamId(new Team("Platform", "desc", creator));
        when(teamRepository.save(any(Team.class))).thenReturn(savedTeam);
        when(teamMapper.toResponse(savedTeam)).thenReturn(dummyTeamResponse(savedTeam));

        teamService.createTeam(new CreateTeamRequest("Platform", "desc"), creator);

        ArgumentCaptor<TeamMember> captor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getTeamRole()).isEqualTo(TeamRole.OWNER);
        assertThat(captor.getValue().getUser()).isEqualTo(creator);
    }

    @Test
    void getTeam_returnsTeam_whenGuardGrantsAccess() {
        User member = withUserId(new User("member@taskflow.io", "hash", "Member", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", member));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(dummyTeamResponse(team));

        TeamResponse response = teamService.getTeam(team.getId(), member);

        assertThat(response).isNotNull();
        verify(teamAccessGuard).requireMembership(team.getId(), member);
    }

    @Test
    void getTeam_propagatesGuardRejection() {
        User outsider = withUserId(new User("outsider@taskflow.io", "hash", "Out", "Sider", Role.DEVELOPER));
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        org.mockito.Mockito.doThrow(new com.taskflow.common.exception.ForbiddenOperationException("nope"))
                .when(teamAccessGuard).requireMembership(team.getId(), outsider);

        assertThrows(com.taskflow.common.exception.ForbiddenOperationException.class,
                () -> teamService.getTeam(team.getId(), outsider));
    }

    @Test
    void addMember_throwsDuplicateWhenUserAlreadyOnTeam() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        User newUser = withUserId(new User("new@taskflow.io", "hash", "New", "User", Role.DEVELOPER));

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));
        when(teamMemberRepository.existsByTeamIdAndUserId(team.getId(), newUser.getId())).thenReturn(true);

        AddTeamMemberRequest request = new AddTeamMemberRequest(newUser.getEmail(), TeamRole.MEMBER);

        assertThrows(DuplicateResourceException.class, () -> teamService.addMember(team.getId(), request, owner));
        verify(teamAccessGuard).requireManageAccess(team.getId(), owner);
    }

    @Test
    void removeMember_preventsRemovingOwner() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        TeamMember ownership = new TeamMember(team, owner, TeamRole.OWNER);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId()))
                .thenReturn(Optional.of(ownership));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.removeMember(team.getId(), owner.getId(), owner));
    }

    @Test
    void removeMember_preventsManagerFromRemovingManager() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        User manager = withUserId(new User("manager@taskflow.io", "hash", "Manager", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        TeamMember ownership = new TeamMember(team, owner, TeamRole.OWNER);
        TeamMember managerMembership = new TeamMember(team, manager, TeamRole.MANAGER);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        org.mockito.Mockito.doNothing().when(teamAccessGuard).requireManageAccess(team.getId(), manager);
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), manager.getId()))
                .thenReturn(Optional.of(managerMembership));
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), manager.getId()))
                .thenReturn(Optional.of(managerMembership));

        assertThrows(IllegalArgumentException.class,
                () -> teamService.removeMember(team.getId(), manager.getId(), manager));
    }

    @Test
    void addMember_preventsManagerFromAssigningManagerRole() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        User manager = withUserId(new User("manager@taskflow.io", "hash", "Manager", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        User newUser = withUserId(new User("new@taskflow.io", "hash", "New", "User", Role.DEVELOPER));
        TeamMember managerMembership = new TeamMember(team, manager, TeamRole.MANAGER);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        org.mockito.Mockito.doNothing().when(teamAccessGuard).requireManageAccess(team.getId(), manager);
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), manager.getId()))
                .thenReturn(Optional.of(managerMembership));

        AddTeamMemberRequest request = new AddTeamMemberRequest(newUser.getEmail(), TeamRole.MANAGER);

        assertThrows(IllegalArgumentException.class, () -> teamService.addMember(team.getId(), request, manager));
    }

    @Test
    void updateTeam_requiresOwnerAccess() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        User manager = withUserId(new User("manager@taskflow.io", "hash", "Manager", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        TeamMember managerMembership = new TeamMember(team, manager, TeamRole.MANAGER);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        org.mockito.Mockito.doThrow(new com.taskflow.common.exception.ForbiddenOperationException("nope"))
                .when(teamAccessGuard).requireOwnerAccess(team.getId(), manager);

        assertThrows(com.taskflow.common.exception.ForbiddenOperationException.class,
                () -> teamService.updateTeam(team.getId(), new com.taskflow.team.dto.UpdateTeamRequest("New Name", "New Desc"), manager));
    }
    @Test
    void updateMemberRole_preventsDemotingTheLastOwner() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        TeamMember ownership = new TeamMember(team, owner, TeamRole.OWNER);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), owner.getId()))
                .thenReturn(Optional.of(ownership));
        when(teamMemberRepository.countByTeamIdAndTeamRole(team.getId(), TeamRole.OWNER)).thenReturn(1L);

        var request = new com.taskflow.team.dto.UpdateTeamMemberRoleRequest(TeamRole.MEMBER);

        assertThrows(IllegalArgumentException.class,
                () -> teamService.updateMemberRole(team.getId(), owner.getId(), request, owner));
    }

    @Test
    void listMembers_returnsMappedMembers() {
        User owner = withUserId(new User("owner@taskflow.io", "hash", "Owner", "User", Role.DEVELOPER));
        Team team = withTeamId(new Team("Platform", "desc", owner));
        TeamMember ownership = new TeamMember(team, owner, TeamRole.OWNER);

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamId(team.getId())).thenReturn(List.of(ownership));
        when(teamMemberMapper.toResponse(ownership)).thenReturn(new TeamMemberResponse(
                UUID.randomUUID(), owner.getId(), owner.getEmail(), owner.getFirstName(),
                owner.getLastName(), TeamRole.OWNER, Instant.now()));

        List<TeamMemberResponse> members = teamService.listMembers(team.getId(), owner);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).teamRole()).isEqualTo(TeamRole.OWNER);
        verify(teamAccessGuard).requireMembership(team.getId(), owner);
    }

    // --- Test helpers ---
    // Team/TeamMember/User ids are JPA-generated (no public setter), so tests
    // assign them via reflection to build fully-formed objects without a persistence context.

    private static User withUserId(User user) {
        setId(user, User.class, UUID.randomUUID());
        return user;
    }

    private static Team withTeamId(Team team) {
        setId(team, Team.class, UUID.randomUUID());
        return team;
    }

    private static void setId(Object target, Class<?> type, UUID id) {
        try {
            Field idField = type.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static TeamResponse dummyTeamResponse(Team team) {
        return new TeamResponse(
                team.getId(), team.getName(), team.getDescription(),
                team.getCreatedBy().getId(), Instant.now(), Instant.now());
    }
}
