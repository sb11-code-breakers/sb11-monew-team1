package com.sprint.mission.monew.batch.article.backup.dto;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleBackupItem(
    UUID id,
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    int commentCount,
    int viewCount,
    Instant createdAt) {

  public static ArticleBackupItem from(Article article) {
    return new ArticleBackupItem(
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
