package com.taskflow.task;

import com.taskflow.task.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = LabelMapper.class)
public interface TaskMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "reporterId", source = "reporter.id")
    TaskResponse toResponse(Task task);
}
