package com.sprint.mission.monew.common.exception;

import java.util.Map;

public abstract class AuthException extends MonewException {

  protected AuthException(ErrorCode errorCode) {
    super(errorCode, Map.of());
  }
}