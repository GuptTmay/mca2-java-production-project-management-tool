package com.taskflow.task;

import com.taskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "task_history")
@EntityListeners(AuditingEntityListener.class)
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private TaskStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    private TaskStatus newStatus;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TaskHistory() {
    }

    public TaskHistory(UUID taskId, UUID projectId, User actor, TaskStatus previousStatus, TaskStatus newStatus, String changeReason) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.actor = actor;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changeReason = changeReason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public User getActor() {
        return actor;
    }

    public TaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskHistory other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
