package com.sprint.mission.monew.batch.exception;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class NotificationCleanupJobFailedException extends MonewInternalException {

  private NotificationCleanupJobFailedException(Throwable cause) {
    super(InternalErrorCode.NOTIFICATION_CLEANUP_JOB_FAILED, null, cause);
  }

  public static NotificationCleanupJobFailedException wrap(Throwable cause) {
    return new NotificationCleanupJobFailedException(cause);
  }
}
