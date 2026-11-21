package com.taskflow.comment;

import com.taskflow.activity.ActivityService;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.comment.dto.CommentRequest;
import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.project.Project;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.project.ProjectStatus;
import com.taskflow.task.Task;
import com.taskflow.task.TaskPriority;
import com.taskflow.task.TaskRepository;
import com.taskflow.task.TaskStatus;
import com.taskflow.team.Team;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectAccessGuard projectAccessGuard;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private ActivityService activityService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository, taskRepository, projectAccessGuard, commentMapper, activityService);
    }

    @Test
    void listComments_requiresProjectViewAccess() {
        User outsider = user(Role.DEVELOPER);
        Task task = task(outsider);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        doThrow(new ForbiddenOperationException("nope"))
                .when(projectAccessGuard).requireViewAccess(task.getProject(), outsider);

        assertThrows(ForbiddenOperationException.class,
                () -> commentService.listComments(task.getId(), outsider));
        verify(commentRepository, never()).findByTaskIdOrderByCreatedAtAsc(any());
    }

    @Test
    void createComment_recordsActivity() {
        User author = user(Role.DEVELOPER);
        Task task = task(author);
        CommentRequest request = new CommentRequest("Nice work!");
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentMapper.toResponse(any(Comment.class))).thenReturn(new CommentResponse(
                UUID.randomUUID(), task.getId(), author.getId(), author.getFirstName(), author.getLastName(),
                author.getEmail(), request.content(), java.time.Instant.now(), java.time.Instant.now()));

        commentService.createComment(task.getId(), request, author);

        verify(activityService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateComment_allowsOnlyAuthor() {
        User author = user(Role.DEVELOPER);
        User other = user(Role.DEVELOPER);
        Task task = task(author);
        Comment comment = comment(task, author);
        CommentRequest request = new CommentRequest("Updated");

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThrows(ForbiddenOperationException.class,
                () -> commentService.updateComment(task.getId(), comment.getId(), request, other));
    }

    @Test
    void deleteComment_allowsManager() {
        User author = user(Role.DEVELOPER);
        User manager = user(Role.PROJECT_MANAGER);
        Task task = task(author);
        Comment comment = comment(task, author);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        commentService.deleteComment(task.getId(), comment.getId(), manager);

        verify(commentRepository).delete(comment);
    }

    // --- Helpers ---

    private static User user(Role role) {
        User u = new User(role.name().toLowerCase() + UUID.randomUUID() + "@taskflow.io", "hash", "First", "Last", role);
        setId(u, User.class, UUID.randomUUID());
        return u;
    }

    private static Task task(User createdBy) {
        Team team = new Team("Platform", "desc", createdBy);
        setId(team, Team.class, UUID.randomUUID());
        Project project = new Project(
                team, "Website Revamp", "desc", com.taskflow.project.ProjectStatus.ACTIVE, null, null, createdBy);
        setId(project, Project.class, UUID.randomUUID());
        Task task = new Task(project, "Fix bug", "desc", com.taskflow.task.TaskStatus.TODO, com.taskflow.task.TaskPriority.MEDIUM, createdBy, createdBy, null);
        setId(task, Task.class, UUID.randomUUID());
        return task;
    }

    private static Comment comment(Task task, User user) {
        Comment c = new Comment(task, user, "Hello");
        setId(c, Comment.class, UUID.randomUUID());
        return c;
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
}
