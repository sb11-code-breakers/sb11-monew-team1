package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.common.exception.InternalErrorCode;
import com.sprint.mission.monew.common.exception.MonewInternalException;

public class LogBackupFailedException extends MonewInternalException {

  private LogBackupFailedException(String s3Key, Throwable cause) {
    super(InternalErrorCode.LOG_BACKUP_FAILED, s3Key, cause);
  }

  public static LogBackupFailedException withKey(String s3Key, Throwable cause) {
    return new LogBackupFailedException(s3Key, cause);
  }
}