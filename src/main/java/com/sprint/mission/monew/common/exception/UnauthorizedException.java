package com.sprint.mission.monew.common.exception;

public class UnauthorizedException extends AuthException {

  private UnauthorizedException() {
    super(ErrorCode.UNAUTHORIZED);
  }

  public static UnauthorizedException of() {
    return new UnauthorizedException();
  }
}