package com.taskflow.activity;

import com.taskflow.activity.dto.ActivityResponse;
import com.taskflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "projectId", source = "projectId")
    @Mapping(target = "actorId", source = "actor.id")
    @Mapping(target = "actorFirstName", source = "actor.firstName")
    @Mapping(target = "actorLastName", source = "actor.lastName")
    @Mapping(target = "actorEmail", source = "actor.email")
    ActivityResponse toResponse(Activity activity);
}
