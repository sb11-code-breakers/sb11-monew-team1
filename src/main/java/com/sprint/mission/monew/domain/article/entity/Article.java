package com.sprint.mission.monew.domain.article.entity;

import com.sprint.mission.monew.common.entity.BaseSoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseSoftDeletableEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ArticleSource source;

  @Column(nullable = false, length = 2048)
  private String sourceUrl;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false)
  private Instant publishDate;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(nullable = false)
  private int commentCount = 0;

  @Column(nullable = false)
  private int viewCount = 0;

  public static Article create(
      ArticleSource source, String sourceUrl, String title, Instant publishDate, String summary) {
    Article article = new Article();
    article.source = source;
    article.sourceUrl = sourceUrl;
    article.title = title;
    article.publishDate = publishDate;
    article.summary = summary;
    return article;
  }

  public void incrementViewCount() {
    this.viewCount++;
  }
}
