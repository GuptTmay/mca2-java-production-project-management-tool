package com.taskflow.project;

import com.taskflow.project.dto.ProjectResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "createdBy", source = "createdBy.id")
    ProjectResponse toResponse(Project project);
}
