package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidPasswordResetCodeException extends UserException {

  private InvalidPasswordResetCodeException(UserErrorCode errorCode, Map<String, Object> details) {
    super(HttpStatus.BAD_REQUEST, errorCode, details);
  }

  public static InvalidPasswordResetCodeException withCode(String code) {
    return new InvalidPasswordResetCodeException(
        UserErrorCode.USER_INVALID_PASSWORD_RESET_CODE,
        Map.of("code", "REDACTED"));
  }
}