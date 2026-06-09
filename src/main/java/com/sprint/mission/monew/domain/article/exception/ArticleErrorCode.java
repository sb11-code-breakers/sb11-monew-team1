package com.sprint.mission.monew.domain.article.exception;

import com.sprint.mission.monew.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArticleErrorCode implements ErrorCode {

  ARTICLE_NOT_FOUND("뉴스 기사를 찾을 수 없습니다.");

  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
