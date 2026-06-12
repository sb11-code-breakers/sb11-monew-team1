package com.sprint.mission.monew.domain.useractivity.document;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentCommentLike {

  private UUID id;
  private Instant createdAt;
  private UUID commentId;
  private UUID articleId;
  private String articleTitle;
  private UUID commentUserId;
  private String commentUserNickname;
  private String commentContent;
  private long commentLikeCount;
  private Instant commentCreatedAt;

  public static RecentCommentLike of(
      UUID likeId, Instant likedAt,
      UUID commentId, UUID articleId, String articleTitle,
      UUID commentUserId, String commentUserNickname, String commentContent,
      long commentLikeCount, Instant commentCreatedAt) {

    RecentCommentLike doc = new RecentCommentLike();
    doc.id = likeId;
    doc.createdAt = likedAt;
    doc.commentId = commentId;
    doc.articleId = articleId;
    doc.articleTitle = articleTitle;
    doc.commentUserId = commentUserId;
    doc.commentUserNickname = commentUserNickname;
    doc.commentContent = commentContent;
    doc.commentLikeCount = commentLikeCount;
    doc.commentCreatedAt = commentCreatedAt;
    return doc;
  }
}
