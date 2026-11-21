package com.taskflow.comment;

import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userFirstName", source = "user.firstName")
    @Mapping(target = "userLastName", source = "user.lastName")
    @Mapping(target = "userEmail", source = "user.email")
    CommentResponse toResponse(Comment comment);
}
