package com.sprint.mission.monew.domain.comment.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class CommentLikeNotFoundException extends CommentException {

  private CommentLikeNotFoundException(Map<String, Object> details) {
    super(ErrorCode.COMMENT_LIKE_NOT_FOUND, details);
  }

  public static CommentLikeNotFoundException withId(UUID userId, UUID commentId) {
    return new CommentLikeNotFoundException(Map.of("userId", userId, "commentId", commentId));
  }
}
