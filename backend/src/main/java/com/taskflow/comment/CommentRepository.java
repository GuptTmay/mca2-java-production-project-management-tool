package com.taskflow.comment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @EntityGraph(attributePaths = "user")
    List<Comment> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    long countByTaskId(UUID taskId);
}
