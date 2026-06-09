package com.sprint.mission.monew.domain.notification.exception;

import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class NotificationException extends MonewException {

  protected NotificationException(
      HttpStatus status,
      NotificationErrorCode errorCode,
      Map<String, Object> details
  ) {
    super(status, errorCode, details);
  }
}