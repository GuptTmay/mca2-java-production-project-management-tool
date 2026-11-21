package com.taskflow.project;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.common.exception.DuplicateResourceException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.project.dto.AddProjectMemberRequest;
import com.taskflow.project.dto.CreateProjectRequest;
import com.taskflow.project.dto.ProjectMemberResponse;
import com.taskflow.project.dto.ProjectResponse;
import com.taskflow.project.dto.UpdateProjectRequest;
import com.taskflow.team.Team;
import com.taskflow.team.TeamAccessGuard;
import com.taskflow.team.TeamRepository;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamAccessGuard teamAccessGuard;
    private final ProjectAccessGuard projectAccessGuard;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            TeamAccessGuard teamAccessGuard,
            ProjectAccessGuard projectAccessGuard,
            ProjectMapper projectMapper,
            ProjectMemberMapper projectMemberMapper) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.teamAccessGuard = teamAccessGuard;
        this.projectAccessGuard = projectAccessGuard;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, User currentUser) {
        validateDateRange(request.startDate(), request.endDate());
        teamAccessGuard.requireManageAccess(request.teamId(), currentUser);

        String code = request.code().trim().toUpperCase();
        if (projectRepository.existsByCode(code)) {
            throw new DuplicateResourceException("A project with this code already exists");
        }

        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> ResourceNotFoundException.of("Team", request.teamId()));

        Project project = new Project(
                team,
                request.name().trim(),
                code,
                request.description(),
                request.status() != null ? request.status() : ProjectStatus.PLANNING,
                request.startDate(),
                request.endDate(),
                currentUser);

        Project saved = projectRepository.save(project);

        // The creator is automatically a project member so they can see their own project.
        projectMemberRepository.save(new ProjectMember(saved, currentUser));

        return projectMapper.toResponse(saved);
    }

    public ProjectResponse getProject(UUID projectId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireViewAccess(project, currentUser);
        return projectMapper.toResponse(project);
    }

    public PageResponse<ProjectResponse> listProjects(
            UUID teamId, ProjectStatus status, User currentUser, Pageable pageable) {
        teamAccessGuard.requireMembership(teamId, currentUser);

        Page<Project> page = status != null
                ? projectRepository.findByTeamIdAndStatus(teamId, status, pageable)
                : projectRepository.findByTeamId(teamId, pageable);

        return PageResponse.from(page.map(projectMapper::toResponse));
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);

        validateDateRange(request.startDate(), request.endDate());
        String code = request.code().trim().toUpperCase();
        if (projectRepository.existsByCodeAndIdNot(code, projectId)) {
            throw new DuplicateResourceException("A project with this code already exists");
        }

        project.setName(request.name().trim());
        project.setCode(code);
        project.setDescription(request.description());
        project.setStatus(request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());

        return projectMapper.toResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);
        projectRepository.delete(project);
    }

    public List<ProjectMemberResponse> listMembers(UUID projectId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireViewAccess(project, currentUser);
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(projectMemberMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProjectMemberResponse addMember(UUID projectId, AddProjectMemberRequest request, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);

        String normalizedEmail = request.email().trim().toLowerCase();
        User targetUser = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (!targetUser.isEnabled()) {
            throw new IllegalArgumentException("Disabled users cannot be added to a project");
        }

        // A project member must already belong to the project's team - a project
        // can never have members the team itself doesn't recognize.
        if (!teamAccessGuard.isTeamMember(project.getTeam().getId(), targetUser.getId())) {
            throw new IllegalArgumentException(
                    "User must be a member of the team before being added to one of its projects");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUser.getId())) {
            throw new DuplicateResourceException("This user is already a member of the project");
        }

        ProjectMember member = projectMemberRepository.save(new ProjectMember(project, targetUser));
        return projectMemberMapper.toResponse(member);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId, User currentUser) {
        Project project = findProjectOrThrow(projectId);
        projectAccessGuard.requireManageAccess(project, currentUser);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("This user is not a member of the project"));

        projectMemberRepository.delete(member);
    }

    private Project findProjectOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
    }

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
    }
}
