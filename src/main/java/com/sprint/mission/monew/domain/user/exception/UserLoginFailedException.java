package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class UserLoginFailedException extends UserException {

  private UserLoginFailedException(Map<String, Object> details) {
    super(ErrorCode.USER_INVALID_PASSWORD, details);
  }

  public static UserLoginFailedException withEmail() {
    return new UserLoginFailedException(Map.of("email", ""));
  }

  public static UserLoginFailedException withPassword() {
    return new UserLoginFailedException(Map.of("password", ""));
  }
}