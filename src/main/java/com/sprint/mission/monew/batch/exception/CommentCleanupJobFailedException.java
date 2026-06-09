package com.sprint.mission.monew.batch.exception;

public class CommentCleanupJobFailedException extends BatchException {

  private CommentCleanupJobFailedException(Throwable cause) {
    super(BatchErrorCode.COMMENT_CLEANUP_JOB_FAILED, null, cause);
  }

  public static CommentCleanupJobFailedException wrap(Throwable cause) {
    return new CommentCleanupJobFailedException(cause);
  }
}
