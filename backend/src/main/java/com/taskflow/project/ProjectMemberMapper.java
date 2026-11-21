package com.taskflow.project;

import com.taskflow.project.dto.ProjectMemberResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFirstName", source = "user.firstName")
    @Mapping(target = "userLastName", source = "user.lastName")
    ProjectMemberResponse toResponse(ProjectMember member);
}
