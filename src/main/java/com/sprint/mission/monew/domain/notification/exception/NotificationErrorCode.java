package com.sprint.mission.monew.domain.notification.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다.");

  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
