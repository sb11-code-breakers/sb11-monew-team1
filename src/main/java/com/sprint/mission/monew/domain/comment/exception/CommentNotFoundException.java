package com.sprint.mission.monew.domain.comment.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CommentNotFoundException extends CommentException {

  private CommentNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, CommentErrorCode.COMMENT_NOT_FOUND, details);
  }

  public static CommentNotFoundException withId(UUID commentId) {
    return new CommentNotFoundException(Map.of("commentId", commentId));
  }
}