package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class UserInvalidPasswordException extends UserException {

  private UserInvalidPasswordException(Map<String, Object> details) {
    super(ErrorCode.USER_INVALID_PASSWORD, details);
  }

  public static UserInvalidPasswordException withoutDetail() {
    return new UserInvalidPasswordException(Map.of("password", ""));
  }
}