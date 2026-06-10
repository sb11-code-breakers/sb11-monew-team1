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
  private Instant commentCreatedAt; // 원본 댓글 작성 시간
  private Instant likedAt;          // 이 유저가 좋아요를 누른 시점

  public static RecentCommentLike of(
      UUID commentId, UUID articleId, String articleTitle,
      Instant commentCreatedAt, Instant likedAt) { // 💡 commentUserId 제거

    RecentCommentLike doc = new RecentCommentLike();
    doc.commentId = commentId;
    doc.articleId = commentId;
    doc.articleTitle = articleTitle;
    doc.commentCreatedAt = commentCreatedAt;
    doc.likedAt = likedAt;
    return doc;
  }
}