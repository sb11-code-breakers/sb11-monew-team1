package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentCommentLike {

  private UUID commentId;
  private UUID articleId;
  private String articleTitle;
  private Instant commentCreatedAt;
  private Instant likedAt;

  public static RecentCommentLike of(
      UUID commentId, UUID articleId, String articleTitle,
      Instant commentCreatedAt, Instant likedAt) {

    RecentCommentLike doc = new RecentCommentLike();
    doc.commentId = commentId;
    doc.articleId = articleId;
    doc.articleTitle = articleTitle;
    doc.commentCreatedAt = commentCreatedAt;
    doc.likedAt = likedAt;
    return doc;
  }
}