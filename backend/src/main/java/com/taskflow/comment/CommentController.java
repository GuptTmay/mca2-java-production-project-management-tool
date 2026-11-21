package com.taskflow.comment;

import com.taskflow.comment.dto.CommentRequest;
import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.user.User;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID taskId, @AuthenticationPrincipal User currentUser) {
        return commentService.listComments(taskId, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable UUID taskId,
                                 @Valid @RequestBody CommentRequest request,
                                 @AuthenticationPrincipal User currentUser) {
        return commentService.createComment(taskId, request, currentUser);
    }

    @PatchMapping("/{commentId}")
    public CommentResponse update(@PathVariable UUID taskId,
                                  @PathVariable UUID commentId,
                                  @Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal User currentUser) {
        return commentService.updateComment(taskId, commentId, request, currentUser);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID taskId,
                       @PathVariable UUID commentId,
                       @AuthenticationPrincipal User currentUser) {
        commentService.deleteComment(taskId, commentId, currentUser);
    }
}
