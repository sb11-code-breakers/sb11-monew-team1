package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserLoginFailedException extends UserException {

  private UserLoginFailedException(Map<String, Object> details) {
    super(HttpStatus.UNAUTHORIZED, UserErrorCode.USER_INVALID_PASSWORD, details);
  }

  public static UserLoginFailedException withEmail() {
    return new UserLoginFailedException(Map.of("email", ""));
  }

  public static UserLoginFailedException withPassword() {
    return new UserLoginFailedException(Map.of("password", ""));
  }
}