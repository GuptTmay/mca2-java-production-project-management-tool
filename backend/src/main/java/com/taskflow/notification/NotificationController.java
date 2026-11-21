package com.taskflow.notification;

import com.taskflow.common.dto.PageResponse;
import com.taskflow.notification.dto.NotificationResponse;
import com.taskflow.notification.dto.UnreadCountResponse;
import com.taskflow.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@AuthenticationPrincipal User currentUser,
                                                   @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                                                   @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return notificationService.listForUser(currentUser.getId(), pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return new UnreadCountResponse(count);
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return notificationService.markAsRead(id, currentUser.getId());
    }

    @PostMapping("/mark-all-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
    }
}

