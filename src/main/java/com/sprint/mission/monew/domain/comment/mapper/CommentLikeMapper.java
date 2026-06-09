package com.sprint.mission.monew.domain.comment.mapper;

import com.sprint.mission.monew.domain.comment.dto.CommentLikeResponse;
import com.sprint.mission.monew.domain.comment.entity.CommentLike;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentLikeMapper {

  @Mapping(target = "likedBy", expression = "java(commentLike.getUser() != null ? commentLike.getUser().getId() : null)")
  @Mapping(target = "commentId", expression = "java(commentLike.getComment().getId())")
  @Mapping(target = "articleId", expression = "java(commentLike.getComment().getArticle().getId())")
  @Mapping(target = "commentUserId", expression = "java(commentLike.getComment().getUser() != null ? commentLike.getComment().getUser().getId() : null)")
  @Mapping(target = "commentUserNickname", expression = "java(commentLike.getComment().getUser() != null ? commentLike.getComment().getUser().getNickname() : \"알 수 없음\")")
  @Mapping(target = "commentContent", expression = "java(commentLike.getComment().getContent())")
  @Mapping(target = "commentLikeCount", source = "likeCount")
  @Mapping(target = "commentCreatedAt", expression = "java(commentLike.getComment().getCreatedAt())")
  CommentLikeResponse toResponse(CommentLike commentLike, long likeCount);

}
