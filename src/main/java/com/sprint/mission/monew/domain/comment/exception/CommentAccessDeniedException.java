package com.sprint.mission.monew.domain.comment.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class CommentAccessDeniedException extends CommentException {

  private CommentAccessDeniedException(Map<String, Object> details) {
    super(ErrorCode.COMMENT_ACCESS_DENIED, details);
  }

  public static CommentAccessDeniedException withId(UUID commentId) {
    return new CommentAccessDeniedException(Map.of("commentId", commentId));
  }
}
