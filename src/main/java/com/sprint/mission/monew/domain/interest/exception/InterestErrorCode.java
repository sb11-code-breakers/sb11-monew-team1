package com.sprint.mission.monew.domain.interest.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InterestErrorCode implements ErrorCode {

  INTEREST_NOT_FOUND("관심사를 찾을 수 없습니다."),
  INTEREST_ALREADY_EXISTS("유사한 관심사가 이미 존재합니다."),
  SUBSCRIPTION_NOT_FOUND("구독 정보를 찾을 수 없습니다."),
  SUBSCRIPTION_ALREADY_EXISTS("이미 구독 중인 관심사입니다.");

  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
