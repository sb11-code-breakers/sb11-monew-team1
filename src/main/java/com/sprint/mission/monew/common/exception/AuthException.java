package com.sprint.mission.monew.common.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class AuthException extends MonewException {

  protected AuthException(HttpStatus status, CommonErrorCode errorCode) {
    super(status, errorCode, Map.of());
  }
}