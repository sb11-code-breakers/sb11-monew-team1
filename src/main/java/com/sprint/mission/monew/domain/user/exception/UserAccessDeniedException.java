package com.sprint.mission.monew.domain.user.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserAccessDeniedException extends UserException {

  private UserAccessDeniedException(Map<String, Object> details) {
    super(HttpStatus.FORBIDDEN, UserErrorCode.USER_ACCESS_DENIED, details);
  }

  public static UserAccessDeniedException forUser(UUID userId) {
    return new UserAccessDeniedException(Map.of("userId", userId));
  }
}