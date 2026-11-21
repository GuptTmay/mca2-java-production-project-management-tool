package com.taskflow.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    boolean existsByName(String name);

    List<Label> findAllByIdIn(List<UUID> ids);
}
