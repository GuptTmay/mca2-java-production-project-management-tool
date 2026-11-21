package com.taskflow.activity;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    @EntityGraph(attributePaths = "actor")
    List<Activity> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    @EntityGraph(attributePaths = "actor")
    List<Activity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
