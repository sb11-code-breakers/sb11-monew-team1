package com.sprint.mission.monew.common.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends AuthException {

  private TooManyRequestsException() {
    super(HttpStatus.TOO_MANY_REQUESTS, CommonErrorCode.TOO_MANY_REQUESTS);
  }

  public static TooManyRequestsException of() {
    return new TooManyRequestsException();
  }
}