package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class UserEmailNotVerifiedException extends UserException {

  private UserEmailNotVerifiedException(Map<String, Object> details) {
    super(ErrorCode.USER_EMAIL_NOT_VERIFIED, details);
  }

  public static UserEmailNotVerifiedException withEmail(String email) {
    return new UserEmailNotVerifiedException(Map.of("email", email));
  }
}