package com.sprint.mission.monew.domain.comment.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class CommentLikeAlreadyExistsException extends CommentException {

  private CommentLikeAlreadyExistsException(Map<String, Object> details) {
    super(ErrorCode.COMMENT_LIKE_ALREADY_EXISTS, details);
  }

  public static CommentLikeAlreadyExistsException withId(UUID userId, UUID commentId) {
    return new CommentLikeAlreadyExistsException(Map.of("userId", userId, "commentId", commentId));
  }
}
