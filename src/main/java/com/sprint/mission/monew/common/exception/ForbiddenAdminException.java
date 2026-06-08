package com.sprint.mission.monew.common.exception;

public class ForbiddenAdminException extends AuthException {

  private ForbiddenAdminException() {
    super(ErrorCode.FORBIDDEN_ADMIN);
  }

  public static ForbiddenAdminException of() {
    return new ForbiddenAdminException();
  }
}