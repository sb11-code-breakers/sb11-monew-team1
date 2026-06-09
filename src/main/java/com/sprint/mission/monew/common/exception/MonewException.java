package com.sprint.mission.monew.common.exception;

import java.util.Map;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class MonewException extends RuntimeException {

  private final HttpStatus status;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  protected MonewException(HttpStatus status,
      ErrorCode errorCode,
      Map<String, Object> details
  ) {
    super(errorCode.getMessage());
    this.status = status;
    this.errorCode = errorCode;
    this.details = details;
  }
}