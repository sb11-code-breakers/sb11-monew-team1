package com.sprint.mission.monew.batch.dto;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleBackupDto(
    UUID id,
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    int commentCount,
    int viewCount,
    Instant createdAt) {

  public static ArticleBackupDto from(Article article) {
    return new ArticleBackupDto(
        article.getId(),
        article.getSource(),
        article.getSourceUrl(),
        article.getTitle(),
        article.getPublishDate(),
        article.getSummary(),
        article.getCommentCount(),
        article.getViewCount(),
        article.getCreatedAt());
  }
}
