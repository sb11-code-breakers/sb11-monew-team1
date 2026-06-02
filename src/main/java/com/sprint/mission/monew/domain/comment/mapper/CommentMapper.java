package com.sprint.mission.monew.domain.comment.mapper;

import com.sprint.mission.monew.domain.comment.dto.CommentResponse;
import com.sprint.mission.monew.domain.comment.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

  @Mapping(target = "articleId", expression = "java(comment.getArticle().getId())")
  @Mapping(target = "userId", expression = "java(comment.getUser() != null ? comment.getUser().getId() : null)")
  @Mapping(target = "userNickname", expression = "java(comment.getUser() != null ? comment.getUser().getNickname() : \"알 수 없음\")")
  @Mapping(target = "likedByMe", source = "likedByMe")
  CommentResponse toResponse(Comment comment, boolean likedByMe);
}
