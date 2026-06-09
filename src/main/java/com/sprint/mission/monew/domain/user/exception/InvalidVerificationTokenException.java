package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class InvalidVerificationTokenException extends UserException {

  private InvalidVerificationTokenException(Map<String, Object> details) {
    super(HttpStatus.BAD_REQUEST, UserErrorCode.USER_INVALID_EMAIL_VERIFICATION_TOKEN, details);
  }

  public static InvalidVerificationTokenException withToken(String token) {
    return new InvalidVerificationTokenException(Map.of("token", token));
  }
}