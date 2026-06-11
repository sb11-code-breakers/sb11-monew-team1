package com.sprint.mission.monew.batch.common.exception;

import lombok.Getter;

@Getter
public abstract class BatchException extends RuntimeException {

  private final BatchErrorCode errorCode;

  protected BatchException(BatchErrorCode errorCode, String detail, Throwable cause) {
    super((detail == null || detail.isEmpty())
        ? errorCode.getMessage()
        : errorCode.getMessage() + ": " + detail, cause);
    this.errorCode = errorCode;
  }
}