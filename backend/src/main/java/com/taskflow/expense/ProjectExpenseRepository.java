package com.taskflow.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectExpenseRepository extends JpaRepository<ProjectExpense, UUID> {

    boolean existsByProjectId(UUID projectId);

    List<ProjectExpense> findByProjectIdOrderBySpentOnDescCreatedAtDesc(UUID projectId);

    Optional<ProjectExpense> findByIdAndProjectId(UUID id, UUID projectId);
}
