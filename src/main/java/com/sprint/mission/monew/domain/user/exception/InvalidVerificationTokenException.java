package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;

public class InvalidVerificationTokenException extends UserException {

  private InvalidVerificationTokenException(Map<String, Object> details) {
    super(ErrorCode.EMAIL_VERIFICATION_INVALID_TOKEN, details);
  }

  public static InvalidVerificationTokenException withToken(String token) {
    return new InvalidVerificationTokenException(Map.of("token", token));
  }
}