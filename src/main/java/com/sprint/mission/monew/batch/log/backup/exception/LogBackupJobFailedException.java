package com.sprint.mission.monew.batch.log.backup.exception;

import com.sprint.mission.monew.batch.common.exception.BatchErrorCode;
import com.sprint.mission.monew.batch.common.exception.BatchException;

public class LogBackupJobFailedException extends BatchException {

  private LogBackupJobFailedException(Throwable cause) {
    super(BatchErrorCode.LOG_BACKUP_JOB_FAILED, null, cause);
  }

  public static LogBackupJobFailedException wrap(Throwable cause) {
    return new LogBackupJobFailedException(cause);
  }
}
