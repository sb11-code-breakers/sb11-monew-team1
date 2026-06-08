package com.sprint.mission.monew.common.exception;

import java.util.Map;

public class UnauthorizedException extends MonewException {

  private UnauthorizedException() {
    super(ErrorCode.UNAUTHORIZED, Map.of());
  }

  public static UnauthorizedException of() {
    return new UnauthorizedException();
  }
}
