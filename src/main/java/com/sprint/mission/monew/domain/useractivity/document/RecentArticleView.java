package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentArticleView {

  private UUID id;
  private UUID viewedBy;
  private Instant createdAt;
  private UUID articleId;
  private String source;
  private String sourceUrl;
  private String articleTitle;
  private Instant articlePublishedDate;
  private String articleSummary;
  private long articleCommentCount;
  private long articleViewCount;

  public static RecentArticleView of(
      UUID articleViewId, UUID viewedBy, Instant viewedAt,
      UUID articleId, String source, String sourceUrl, String articleTitle,
      Instant articlePublishedDate, String articleSummary,
      long articleCommentCount, long articleViewCount) {

    RecentArticleView doc = new RecentArticleView();
    doc.id = articleViewId;
    doc.viewedBy = viewedBy;
    doc.createdAt = viewedAt;
    doc.articleId = articleId;
    doc.source = source;
    doc.sourceUrl = sourceUrl;
    doc.articleTitle = articleTitle;
    doc.articlePublishedDate = articlePublishedDate;
    doc.articleSummary = articleSummary;
    doc.articleCommentCount = articleCommentCount;
    doc.articleViewCount = articleViewCount;
    return doc;
  }
}
