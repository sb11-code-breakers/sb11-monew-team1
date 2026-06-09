package com.sprint.mission.monew.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

  FORBIDDEN_ADMIN("관리자만 접근 가능합니다."),
  UNAUTHORIZED("인증되지 않은 사용자입니다."),
  RESOURCE_NOT_FOUND("요청한 리소스를 찾을 수 없습니다."),
  METHOD_NOT_ALLOWED("지원하지 않는 HTTP 메서드입니다."),
  MESSAGE_NOT_READABLE("요청 본문을 읽을 수 없습니다."),
  TYPE_MISMATCH("요청 파라미터의 타입이 올바르지 않습니다."),
  VALIDATION_ERROR("입력값이 올바르지 않습니다."),
  INTERNAL_ERROR("서버 내부 오류가 발생했습니다.");


  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
