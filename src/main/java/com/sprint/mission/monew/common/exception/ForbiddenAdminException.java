package com.sprint.mission.monew.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenAdminException extends AuthException {

  private ForbiddenAdminException() {
    super(HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN_ADMIN);
  }

  public static ForbiddenAdminException of() {
    return new ForbiddenAdminException();
  }
}