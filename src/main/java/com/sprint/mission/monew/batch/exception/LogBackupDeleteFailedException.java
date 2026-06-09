package com.sprint.mission.monew.batch.exception;

import java.nio.file.Path;

public class LogBackupDeleteFailedException extends BatchException {

  private LogBackupDeleteFailedException(Path logFile, Throwable cause) {
    super(BatchErrorCode.LOG_BACKUP_DELETE_FAILED, logFile.toString(), cause);
  }

  public static LogBackupDeleteFailedException withPath(Path logFile, Throwable cause) {
    return new LogBackupDeleteFailedException(logFile, cause);
  }
}