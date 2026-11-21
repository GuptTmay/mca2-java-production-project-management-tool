package com.taskflow.project;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.project.dto.AddProjectMemberRequest;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectMemberResponse;
import com.taskflow.project.dto.ProjectResponse;
import com.taskflow.project.dto.UpdateProjectRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @Valid @RequestBody CreateProjectRequest request, @AuthenticationPrincipal User currentUser) {
        return projectService.createProject(request, currentUser);
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @RequestParam UUID teamId,
            @RequestParam(required = false) ProjectStatus status,
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return projectService.listProjects(teamId, status, currentUser, pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return projectService.getProject(id, currentUser);
    }

    @PatchMapping("/{id}")
    public ProjectResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        return projectService.updateProject(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(id, currentUser);
    }

    @GetMapping("/{id}/members")
    public List<ProjectMemberResponse> listMembers(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return projectService.listMembers(id, currentUser);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddProjectMemberRequest request,
            @AuthenticationPrincipal User currentUser) {
        return projectService.addMember(id, request, currentUser);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User currentUser) {
        projectService.removeMember(id, userId, currentUser);
    }
}
