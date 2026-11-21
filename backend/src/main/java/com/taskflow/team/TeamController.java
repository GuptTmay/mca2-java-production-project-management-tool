package com.taskflow.team;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.team.dto.AddTeamMemberRequest;
import com.taskflow.team.dto.CreateTeamRequest;
import com.taskflow.team.dto.TeamMemberResponse;
import com.taskflow.team.dto.TeamResponse;
import com.taskflow.team.dto.UpdateTeamMemberRoleRequest;
import com.taskflow.team.dto.UpdateTeamRequest;
import com.taskflow.user.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@Valid @RequestBody CreateTeamRequest request, @AuthenticationPrincipal User currentUser) {
        return teamService.createTeam(request, currentUser);
    }

    @GetMapping
    public PageResponse<TeamResponse> list(
            @AuthenticationPrincipal User currentUser, @PageableDefault(size = 20) Pageable pageable) {
        return teamService.listTeams(currentUser, pageable);
    }

    @GetMapping("/{id}")
    public TeamResponse getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return teamService.getTeam(id, currentUser);
    }

    @PatchMapping("/{id}")
    public TeamResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal User currentUser) {
        return teamService.updateTeam(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        teamService.deleteTeam(id, currentUser);
    }

    @GetMapping("/{id}/members")
    public List<TeamMemberResponse> listMembers(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return teamService.listMembers(id, currentUser);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamMemberResponse addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddTeamMemberRequest request,
            @AuthenticationPrincipal User currentUser) {
        return teamService.addMember(id, request, currentUser);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User currentUser) {
        teamService.removeMember(id, userId, currentUser);
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public TeamMemberResponse updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTeamMemberRoleRequest request,
            @AuthenticationPrincipal User currentUser) {
        return teamService.updateMemberRole(id, userId, request, currentUser);
    }
}
