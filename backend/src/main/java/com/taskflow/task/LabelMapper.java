package com.taskflow.task;

import com.taskflow.task.dto.LabelResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabelMapper {

    LabelResponse toResponse(Label label);
}
