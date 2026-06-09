package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserEmailDuplicateException extends UserException {

  private UserEmailDuplicateException(Map<String, Object> details) {
    super(HttpStatus.CONFLICT, UserErrorCode.USER_EMAIL_DUPLICATE, details);
  }

  public static UserEmailDuplicateException withEmail(String email) {
    return new UserEmailDuplicateException(Map.of("email", mask(email)));
  }

  private static String mask(String email) {
    int at = email.indexOf("@");
    if (at <= 0) {
      return "REDACTED";
    }
    return email.charAt(0) + "***" + email.substring(at);
  }
}