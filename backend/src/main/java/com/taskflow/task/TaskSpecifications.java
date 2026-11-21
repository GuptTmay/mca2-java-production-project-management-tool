package com.taskflow.task;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> hasProjectId(UUID projectId) {
        return (root, query, cb) -> projectId == null
                ? null
                : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasAssigneeId(UUID assigneeId) {
        return (root, query, cb) -> assigneeId == null
                ? null
                : cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> deadlineAfter(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("deadline"), from);
    }

    public static Specification<Task> deadlineBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("deadline"), to);
    }

    public static Specification<Task> titleOrDescriptionContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }

    public static Specification<Task> fromFilter(TaskFilter filter) {
        return Specification.where(hasProjectId(filter.projectId()))
                .and(hasStatus(filter.status()))
                .and(hasPriority(filter.priority()))
                .and(hasAssigneeId(filter.assigneeId()))
                .and(deadlineAfter(filter.deadlineFrom()))
                .and(deadlineBefore(filter.deadlineTo()))
                .and(titleOrDescriptionContains(filter.search()));
    }
}
