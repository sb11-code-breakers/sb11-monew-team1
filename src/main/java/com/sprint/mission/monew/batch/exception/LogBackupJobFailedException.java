package com.sprint.mission.monew.batch.exception;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class LogBackupJobFailedException extends MonewInternalException {

  private LogBackupJobFailedException(Throwable cause) {
    super(InternalErrorCode.LOG_BACKUP_JOB_FAILED, null, cause);
  }

  public static LogBackupJobFailedException wrap(Throwable cause) {
    return new LogBackupJobFailedException(cause);
  }
}
