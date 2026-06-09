package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserEmailNotVerifiedException extends UserException {

  private UserEmailNotVerifiedException(Map<String, Object> details) {
    super(HttpStatus.UNAUTHORIZED, UserErrorCode.USER_EMAIL_NOT_VERIFIED, details);
  }

  public static UserEmailNotVerifiedException withEmail(String email) {
    return new UserEmailNotVerifiedException(Map.of("email", email));
  }
}