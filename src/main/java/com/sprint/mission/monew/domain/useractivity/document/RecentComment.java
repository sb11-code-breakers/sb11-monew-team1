package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentComment {

  private UUID commentId;
  private UUID articleId;
  private String articleTitle;
  private Instant createdAt;
  public static RecentComment of(
      UUID commentId, UUID articleId, String articleTitle, Instant createdAt) {

    RecentComment doc = new RecentComment();
    doc.commentId = commentId;
    doc.articleId = articleId;
    doc.articleTitle = articleTitle;
    doc.createdAt = createdAt;
    return doc;
  }
}