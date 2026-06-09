package com.sprint.mission.monew.batch.exception;

public class UserCleanupJobFailedException extends BatchException {

  private UserCleanupJobFailedException(Throwable cause) {
    super(BatchErrorCode.USER_CLEANUP_JOB_FAILED, null, cause);
  }

  public static UserCleanupJobFailedException wrap(Throwable cause) {
    return new UserCleanupJobFailedException(cause);
  }
}
