package com.sprint.mission.monew.batch.exception;

public class LogBackupJobFailedException extends BatchException {

  private LogBackupJobFailedException(Throwable cause) {
    super(BatchErrorCode.LOG_BACKUP_JOB_FAILED, null, cause);
  }

  public static LogBackupJobFailedException wrap(Throwable cause) {
    return new LogBackupJobFailedException(cause);
  }
}
