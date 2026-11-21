package com.taskflow.notification;

import com.taskflow.common.exception.ForbiddenOperationException;
import com.taskflow.common.exception.ResourceNotFoundException;
import com.taskflow.notification.Notification;
import com.taskflow.notification.NotificationType;
import com.taskflow.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, notificationMapper);
    }

    @Test
    void listForUser_returnsPagedNotifications() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Notification> page = new PageImpl<>(List.of(notification(userId)));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(new NotificationResponse(
                UUID.randomUUID(), userId, NotificationType.TASK_ASSIGNED, "msg", null, false, java.time.Instant.now()));

        var result = notificationService.listForUser(userId, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void getUnreadCount_returnsCount() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(3L);

        long count = notificationService.getUnreadCount(userId);

        assertThat(count).isEqualTo(3);
    }

    @Test
    void markAsRead_returnsForbiddenForOtherUser() {
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Notification notification = new Notification(ownerId, NotificationType.TASK_ASSIGNED, "msg", null);
        setId(notification, Notification.class, notificationId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(ForbiddenOperationException.class,
                () -> notificationService.markAsRead(notificationId, otherId));
    }

    @Test
    void create_savesNotification() {
        UUID userId = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse(UUID.randomUUID(), userId, NotificationType.TASK_ASSIGNED, "msg", null, false, java.time.Instant.now());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificationMapper.toResponse(any(Notification.class))).thenReturn(response);

        var result = notificationService.create(userId, NotificationType.TASK_ASSIGNED, "msg", null);

        assertThat(result).isEqualTo(response);
        verify(notificationRepository).save(any(Notification.class));
    }

    private static Notification notification(UUID userId) {
        return new Notification(userId, NotificationType.TASK_ASSIGNED, "You were assigned", UUID.randomUUID());
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
