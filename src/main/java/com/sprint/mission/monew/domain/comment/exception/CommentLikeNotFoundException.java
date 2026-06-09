package com.sprint.mission.monew.domain.comment.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CommentLikeNotFoundException extends CommentException {

  private CommentLikeNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, CommentErrorCode.COMMENT_LIKE_NOT_FOUND, details);
  }

  public static CommentLikeNotFoundException withId(UUID userId, UUID commentId) {
    return new CommentLikeNotFoundException(Map.of("userId", userId, "commentId", commentId));
  }
}
