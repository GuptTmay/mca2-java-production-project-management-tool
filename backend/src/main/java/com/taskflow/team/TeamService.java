package com.taskflow.team;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.team.dto.AddTeamMemberRequest;
import com.taskflow.team.dto.CreateTeamRequest;
import com.taskflow.team.dto.TeamMemberResponse;
import com.taskflow.team.dto.TeamResponse;
import com.taskflow.team.dto.UpdateTeamMemberRoleRequest;
import com.taskflow.team.dto.UpdateTeamRequest;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamAccessGuard teamAccessGuard;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            TeamMapper teamMapper,
            TeamMemberMapper teamMemberMapper,
            TeamAccessGuard teamAccessGuard) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.teamMapper = teamMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.teamAccessGuard = teamAccessGuard;
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, User currentUser) {
        Team team = new Team(request.name().trim(), request.description(), currentUser);
        Team saved = teamRepository.save(team);

        // The creator is always the first OWNER - a team can never be created "orphaned".
        teamMemberRepository.save(new TeamMember(saved, currentUser, TeamRole.OWNER));

        return teamMapper.toResponse(saved);
    }

    public TeamResponse getTeam(UUID teamId, User currentUser) {
        Team team = findTeamOrThrow(teamId);
        teamAccessGuard.requireMembership(teamId, currentUser);
        return teamMapper.toResponse(team);
    }

    public PageResponse<TeamResponse> listTeams(User currentUser, Pageable pageable) {
        var page = currentUser.getRole() == Role.ADMIN
                ? teamRepository.findAll(pageable)
                : teamRepository.findAllByMemberUserId(currentUser.getId(), pageable);
        return PageResponse.from(page.map(teamMapper::toResponse));
    }

    @Transactional
    public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request, User currentUser) {
        Team team = findTeamOrThrow(teamId);
        teamAccessGuard.requireOwnerAccess(teamId, currentUser);

        team.setName(request.name().trim());
        team.setDescription(request.description());
        return teamMapper.toResponse(team);
    }

    @Transactional
    public void deleteTeam(UUID teamId, User currentUser) {
        Team team = findTeamOrThrow(teamId);
        teamAccessGuard.requireOwnerAccess(teamId, currentUser);
        teamRepository.delete(team);
    }

    public List<TeamMemberResponse> listMembers(UUID teamId, User currentUser) {
        findTeamOrThrow(teamId);
        teamAccessGuard.requireMembership(teamId, currentUser);
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(teamMemberMapper::toResponse)
                .toList();
    }

    @Transactional
    public TeamMemberResponse addMember(UUID teamId, AddTeamMemberRequest request, User currentUser) {
        Team team = findTeamOrThrow(teamId);
        teamAccessGuard.requireManageAccess(teamId, currentUser);

        if (request.teamRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("The Owner role is reserved for the team creator and cannot be assigned to other members");
        }

        if (!isTeamOwner(teamId, currentUser.getId()) && request.teamRole() != TeamRole.MEMBER) {
            throw new IllegalArgumentException("Only the team owner can assign the Manager role");
        }

        User targetUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, targetUser.getId())) {
            throw new DuplicateResourceException("This user is already a member of the team");
        }

        TeamMember member = teamMemberRepository.save(new TeamMember(team, targetUser, request.teamRole()));
        return teamMemberMapper.toResponse(member);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId, User currentUser) {
        findTeamOrThrow(teamId);
        teamAccessGuard.requireManageAccess(teamId, currentUser);

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("This user is not a member of the team"));

        if (member.getTeamRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the team owner");
        }

        if (!isTeamOwner(teamId, currentUser.getId()) && member.getTeamRole() != TeamRole.MEMBER) {
            throw new IllegalArgumentException("Only the team owner can remove managers");
        }

        teamMemberRepository.delete(member);
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(
            UUID teamId, UUID userId, UpdateTeamMemberRoleRequest request, User currentUser) {
        findTeamOrThrow(teamId);
        teamAccessGuard.requireOwnerAccess(teamId, currentUser);

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("This user is not a member of the team"));

        boolean demotingLastOwner = member.getTeamRole() == TeamRole.OWNER
                && request.teamRole() != TeamRole.OWNER
                && teamMemberRepository.countByTeamIdAndTeamRole(teamId, TeamRole.OWNER) <= 1;
        if (demotingLastOwner) {
            throw new IllegalArgumentException("Cannot demote the last owner of a team");
        }

        member.setTeamRole(request.teamRole());
        return teamMemberMapper.toResponse(member);
    }

    private Team findTeamOrThrow(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", teamId));
    }

    private boolean isTeamOwner(UUID teamId, UUID userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .map(member -> member.getTeamRole() == TeamRole.OWNER)
                .orElse(false);
    }
}
