package com.sprint.mission.monew.batch.exception;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;
import java.nio.file.Path;

public class LogBackupDeleteFailedException extends MonewInternalException {

  private LogBackupDeleteFailedException(Path logFile, Throwable cause) {
    super(InternalErrorCode.LOG_BACKUP_DELETE_FAILED, logFile.toString(), cause);
  }

  public static LogBackupDeleteFailedException withPath(Path logFile, Throwable cause) {
    return new LogBackupDeleteFailedException(logFile, cause);
  }
}