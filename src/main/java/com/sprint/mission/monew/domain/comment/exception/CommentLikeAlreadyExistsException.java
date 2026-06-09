package com.sprint.mission.monew.domain.comment.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CommentLikeAlreadyExistsException extends CommentException {

  private CommentLikeAlreadyExistsException(Map<String, Object> details) {
    super(HttpStatus.CONFLICT, CommentErrorCode.COMMENT_LIKE_ALREADY_EXISTS, details);
  }

  public static CommentLikeAlreadyExistsException withId(UUID userId, UUID commentId) {
    return new CommentLikeAlreadyExistsException(Map.of("userId", userId, "commentId", commentId));
  }
}