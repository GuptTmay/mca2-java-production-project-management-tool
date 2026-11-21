package com.taskflow.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByTeamId(UUID teamId, Pageable pageable);

    Page<Project> findByTeamIdAndStatus(UUID teamId, ProjectStatus status, Pageable pageable);
}
