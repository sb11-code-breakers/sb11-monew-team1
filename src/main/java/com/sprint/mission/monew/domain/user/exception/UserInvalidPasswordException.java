package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserInvalidPasswordException extends UserException {

  private UserInvalidPasswordException(Map<String, Object> details) {
    super(HttpStatus.UNAUTHORIZED, UserErrorCode.USER_INVALID_PASSWORD, details);
  }

  public static UserInvalidPasswordException withoutDetail() {
    return new UserInvalidPasswordException(Map.of("password", ""));
  }
}