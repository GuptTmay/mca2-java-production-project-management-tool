package com.taskflow.task;

import com.taskflow.task.dto.TaskHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskHistoryMapper {

    @Mapping(target = "actorId", source = "actor.id")
    @Mapping(target = "actorName", expression = "java(history.getActor().getFirstName() + \" \" + history.getActor().getLastName())")
    TaskHistoryResponse toResponse(TaskHistory history);
}
