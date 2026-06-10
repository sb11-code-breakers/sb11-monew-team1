package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentArticleView {

  private UUID articleId;
  private String source;
  private String sourceUrl;
  private String articleTitle;
  private Instant articlePublishedDate;
  private String articleSummary;
  private Instant viewedAt; // 유저가 기사를 조회한 시점

  public static RecentArticleView of(
      UUID articleId, String source, String sourceUrl, String articleTitle,
      Instant articlePublishedDate, String articleSummary, Instant viewedAt) {

    RecentArticleView doc = new RecentArticleView();
    doc.articleId = articleId;
    doc.source = source;
    doc.sourceUrl = sourceUrl;
    doc.articleTitle = articleTitle;
    doc.articlePublishedDate = articlePublishedDate;
    doc.articleSummary = articleSummary;
    doc.viewedAt = viewedAt;
    return doc;
  }
}