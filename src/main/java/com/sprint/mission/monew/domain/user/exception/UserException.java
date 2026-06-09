package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class UserException extends MonewException {

  protected UserException(
      HttpStatus status,
      UserErrorCode errorCode,
      Map<String, Object> details
  ) {
    super(status, errorCode, details);
  }
}