package com.sprint.mission.monew.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // Common
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
  MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
  TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 파라미터의 타입이 올바르지 않습니다."),
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

  // User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  USER_INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
  USER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  USER_EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "이메일 인증이 필요합니다."),
  USER_INVALID_EMAIL_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 인증 토큰입니다."),
  USER_INVALID_PASSWORD_RESET_CODE(HttpStatus.BAD_REQUEST, "유효하지 않거나 만료된 비밀번호 재설정 코드입니다."),

  // Interest
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사를 찾을 수 없습니다."),
  INTEREST_ALREADY_EXISTS(HttpStatus.CONFLICT, "유사한 관심사가 이미 존재합니다."),

  // Subscription
  SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
  SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 구독 중인 관심사입니다."),

  // Article
  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "뉴스 기사를 찾을 수 없습니다."),
  ARTICLE_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서 값의 형식이 올바르지 않습니다."),

  // Comment
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글 수정 권한이 없습니다."),

  // Comment_Like
  COMMENT_LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요한 댓글입니다."),
  COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 찾을 수 없습니다."),

  // Notification
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;
}
