package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// RecentComment.java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentComment {

  private UUID id;
  private UUID articleId;
  private String articleTitle;
  private UUID userId;
  private String userNickname;
  private String content;
  private long likeCount;
  private Instant createdAt;

  public static RecentComment of(
      UUID id, UUID articleId, String articleTitle,
      UUID userId, String userNickname, String content,
      long likeCount, Instant createdAt) {
    RecentComment doc = new RecentComment();
    doc.id = id;
    doc.articleId = articleId;
    doc.articleTitle = articleTitle;
    doc.userId = userId;
    doc.userNickname = userNickname;
    doc.content = content;
    doc.likeCount = likeCount;
    doc.createdAt = createdAt;
    return doc;
  }
}