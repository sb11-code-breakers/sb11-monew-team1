package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.util.MonewUtil;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class UserAccountLockedException extends UserException {

  private UserAccountLockedException(Map<String, Object> details) {
    super(HttpStatus.LOCKED, UserErrorCode.USER_ACCOUNT_LOCKED, details);
  }

  public static UserAccountLockedException withEmail(String email) {
    return new UserAccountLockedException(Map.of("email", MonewUtil.maskEmail(email)));
  }
}