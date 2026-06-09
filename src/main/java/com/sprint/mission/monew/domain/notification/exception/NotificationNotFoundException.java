package com.sprint.mission.monew.domain.notification.exception;


import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends NotificationException {

  private NotificationNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, NotificationErrorCode.NOTIFICATION_NOT_FOUND, details);
  }

  public static NotificationNotFoundException withId(UUID notificationId) {
    return new NotificationNotFoundException(Map.of("notificationId", notificationId));
  }
}