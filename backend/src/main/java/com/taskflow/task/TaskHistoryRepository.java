package com.taskflow.task;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {

    @EntityGraph(attributePaths = "actor")
    List<TaskHistory> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    @EntityGraph(attributePaths = "actor")
    List<TaskHistory> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
