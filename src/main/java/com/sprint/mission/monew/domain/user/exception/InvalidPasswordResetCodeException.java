package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class InvalidPasswordResetCodeException extends UserException {

  private InvalidPasswordResetCodeException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public static InvalidPasswordResetCodeException withCode(String code) {
    return new InvalidPasswordResetCodeException(
        ErrorCode.USER_INVALID_PASSWORD_RESET_CODE,
        Map.of("code", "REDACTED"));
  }
}