package com.taskflow.team;

import com.taskflow.team.dto.TeamResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "createdBy", source = "createdBy.id")
    TeamResponse toResponse(Team team);
}
