package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class InvalidUnlockTokenException extends UserException {

  private InvalidUnlockTokenException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public static InvalidUnlockTokenException withToken(String token) {
    return new InvalidUnlockTokenException(
        ErrorCode.USER_INVALID_UNLOCK_TOKEN,
        Map.of("token", "REDACTED"));
  }
}