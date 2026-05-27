package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class UserEmailDuplicateException extends UserException {

  private UserEmailDuplicateException(Map<String, Object> details) {
    super(ErrorCode.USER_EMAIL_DUPLICATE, details);
  }

  public static UserEmailDuplicateException withEmail(String email) {
    return new UserEmailDuplicateException(Map.of("email", email));
  }
}