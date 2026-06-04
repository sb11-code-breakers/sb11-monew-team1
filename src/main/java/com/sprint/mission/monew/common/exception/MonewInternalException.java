package com.sprint.mission.monew.common.exception;

import lombok.Getter;

@Getter
public abstract class MonewInternalException extends RuntimeException {

  private final InternalErrorCode errorCode;

  protected MonewInternalException(InternalErrorCode errorCode, String detail, Throwable cause) {
    super(errorCode.getMessage() + ": " + detail, cause);
    this.errorCode = errorCode;
  }
}