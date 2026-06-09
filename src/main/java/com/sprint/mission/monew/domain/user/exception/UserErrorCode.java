package com.sprint.mission.monew.domain.user.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
  USER_EMAIL_DUPLICATE("이미 사용 중인 이메일입니다."),
  USER_INVALID_PASSWORD("비밀번호가 올바르지 않습니다."),
  USER_ACCESS_DENIED("접근 권한이 없습니다."),
  USER_EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다."),
  USER_INVALID_EMAIL_VERIFICATION_TOKEN("유효하지 않거나 만료된 인증 토큰입니다."),
  USER_INVALID_UNLOCK_TOKEN("유효하지 않거나 만료된 잠금 해제 토큰입니다."),
  USER_INVALID_PASSWORD_RESET_CODE("유효하지 않거나 만료된 비밀번호 재설정 코드입니다."),
  USER_ACCOUNT_LOCKED("계정이 잠겼습니다. 이메일 인증을 통해 잠금을 해제해주세요."),
  USER_OPTIMISTIC_LOCK_CONFLICT("다른 작업과 충돌하여 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");

  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}