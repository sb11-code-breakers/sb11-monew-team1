package com.sprint.mission.monew.domain.comment.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCode {

  COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다."),
  COMMENT_ACCESS_DENIED("댓글 수정 권한이 없습니다."),
  COMMENT_LIKE_NOT_FOUND("좋아요를 찾을 수 없습니다."),
  COMMENT_LIKE_ALREADY_EXISTS("이미 좋아요한 댓글입니다.");

  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
