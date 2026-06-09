package com.sprint.mission.monew.domain.comment.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CommentAccessDeniedException extends CommentException {

  private CommentAccessDeniedException(Map<String, Object> details) {
    super(HttpStatus.FORBIDDEN, CommentErrorCode.COMMENT_ACCESS_DENIED, details);
  }

  public static CommentAccessDeniedException withId(UUID commentId) {
    return new CommentAccessDeniedException(Map.of("commentId", commentId));
  }
}