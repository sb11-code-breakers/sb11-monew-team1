package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class UserInvalidUnlockTokenException extends UserException {

  private UserInvalidUnlockTokenException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public static UserInvalidUnlockTokenException withToken(String token) {
    return new UserInvalidUnlockTokenException(
        ErrorCode.USER_INVALID_UNLOCK_TOKEN,
        Map.of("token", "REDACTED"));
  }
}