package com.sprint.mission.monew.domain.article.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ArticleNotFoundException extends ArticleException {

  private ArticleNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, ArticleErrorCode.ARTICLE_NOT_FOUND, details);
  }

  public static ArticleNotFoundException withId(UUID articleId) {
    return new ArticleNotFoundException(Map.of("articleId", articleId));
  }
}