package com.sprint.mission.monew.domain.article.exception;

import com.sprint.mission.monew.common.exception.MonewException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ArticleException extends MonewException {

  protected ArticleException(
      HttpStatus status,
      ArticleErrorCode errorCode,
      Map<String, Object> details
  ) {
    super(status, errorCode, details);
  }
}