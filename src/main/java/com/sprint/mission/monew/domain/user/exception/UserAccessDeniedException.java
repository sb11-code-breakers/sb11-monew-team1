package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class UserAccessDeniedException extends UserException {

  private UserAccessDeniedException(Map<String, Object> details) {
    super(ErrorCode.USER_ACCESS_DENIED, details);
  }

  public static UserAccessDeniedException forUser(UUID userId) {
    return new UserAccessDeniedException(Map.of("userId", userId));
  }
}