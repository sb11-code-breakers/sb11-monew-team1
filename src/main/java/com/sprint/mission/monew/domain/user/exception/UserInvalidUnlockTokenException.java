package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserInvalidUnlockTokenException extends UserException {

  private UserInvalidUnlockTokenException(UserErrorCode errorCode, Map<String, Object> details) {
    super(HttpStatus.BAD_REQUEST, errorCode, details);
  }

  public static UserInvalidUnlockTokenException withToken(String token) {
    return new UserInvalidUnlockTokenException(
        UserErrorCode.USER_INVALID_UNLOCK_TOKEN,
        Map.of("token", "REDACTED"));
  }
}