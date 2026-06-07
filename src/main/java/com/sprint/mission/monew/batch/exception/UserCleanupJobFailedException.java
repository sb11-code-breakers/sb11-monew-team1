package com.sprint.mission.monew.batch.exception;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class UserCleanupJobFailedException extends MonewInternalException {

  private UserCleanupJobFailedException(Throwable cause) {
    super(InternalErrorCode.USER_CLEANUP_JOB_FAILED, null, cause);
  }

  public static UserCleanupJobFailedException wrap(Throwable cause) {
    return new UserCleanupJobFailedException(cause);
  }
}
