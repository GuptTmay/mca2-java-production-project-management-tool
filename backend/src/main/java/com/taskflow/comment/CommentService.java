package com.taskflow.comment;

import com.taskflow.activity.ActivityService;
import com.taskflow.activity.ActivityType;
import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.comment.dto.CommentRequest;
import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.project.ProjectAccessGuard;
import com.taskflow.task.Task;
import com.taskflow.task.TaskRepository;
import com.taskflow.user.Role;
import com.taskflow.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final CommentMapper commentMapper;
    private final ActivityService activityService;

    public CommentService(
            CommentRepository commentRepository,
            TaskRepository taskRepository,
            ProjectAccessGuard projectAccessGuard,
            CommentMapper commentMapper,
            ActivityService activityService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.projectAccessGuard = projectAccessGuard;
        this.commentMapper = commentMapper;
        this.activityService = activityService;
    }

    public List<CommentResponse> listComments(UUID taskId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(UUID taskId, CommentRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);

        Comment comment = new Comment(task, currentUser, request.content().trim());
        CommentResponse response = commentMapper.toResponse(commentRepository.save(comment));
        activityService.record(task.getProject().getId(), taskId, currentUser, ActivityType.COMMENT_ADDED,
                currentUser.getFirstName() + " " + currentUser.getLastName() + " commented on this task", null);
        return response;
    }

    @Transactional
    public CommentResponse updateComment(UUID taskId, UUID commentId, CommentRequest request, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("Only the comment author can edit this comment");
        }

        comment.setContent(request.content().trim());
        return commentMapper.toResponse(comment);
    }

    @Transactional
    public void deleteComment(UUID taskId, UUID commentId, User currentUser) {
        Task task = findTaskOrThrow(taskId);
        projectAccessGuard.requireViewAccess(task.getProject(), currentUser);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            if (currentUser.getRole() != Role.ADMIN) {
                try {
                    projectAccessGuard.requireManageAccess(task.getProject(), currentUser);
                } catch (ForbiddenOperationException ex) {
                    throw new ForbiddenOperationException("Only the comment author or a team manager can delete this comment");
                }
            }
        }

        commentRepository.delete(comment);
    }

    private Task findTaskOrThrow(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.of("Task", taskId));
    }
}
