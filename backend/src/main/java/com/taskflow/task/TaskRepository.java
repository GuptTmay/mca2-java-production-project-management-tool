package com.taskflow.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    // Without this EntityGraph, mapping each Task in a page to a TaskResponse
    // (which reads project.id, assignee.id, reporter.id) would trigger up to
    // 3 extra lazy-load queries PER TASK - classic N+1. This fetches all three
    // associations in the same query as the page itself.
    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "reporter", "labels"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "reporter", "labels"})
    Optional<Task> findById(UUID id);

    @EntityGraph(attributePaths = {"project", "assignee", "reporter", "labels"})
    Page<Task> findByAssigneeId(UUID assigneeId, Pageable pageable);

    // --- Dashboard aggregation queries ---
    // All counted in the database rather than loading tasks into memory and
    // counting in Java - the difference between O(1) queries and O(n) rows
    // transferred for a project dashboard.

    long countByProjectId(UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);

    long countByProjectIdAndDeadlineBeforeAndStatusNot(UUID projectId, Instant now, TaskStatus excludedStatus);

    long countByAssigneeId(UUID assigneeId);

    long countByAssigneeIdAndDeadlineBeforeAndStatusNot(UUID assigneeId, Instant now, TaskStatus excludedStatus);

    @Query("select new com.taskflow.task.StatusCount(t.status, count(t)) "
            + "from Task t where t.project.id = :projectId group by t.status")
    List<StatusCount> countByStatusForProject(@Param("projectId") UUID projectId);

    @Query("select new com.taskflow.task.PriorityCount(t.priority, count(t)) "
            + "from Task t where t.project.id = :projectId group by t.priority")
    List<PriorityCount> countByPriorityForProject(@Param("projectId") UUID projectId);

    @Query("select new com.taskflow.task.StatusCount(t.status, count(t)) "
            + "from Task t where t.assignee.id = :userId group by t.status")
    List<StatusCount> countByStatusForAssignee(@Param("userId") UUID userId);
}

