package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecentCommentLikeTest {

  @Nested
  @DisplayName("RecentCommentLike.of()")
  class Of {

    @Test
    @DisplayName("전달한 값으로 RecentCommentLike를 생성한다")
    void 전달한_값으로_RecentCommentLike를_생성한다() {
      // given
      UUID likeId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID commentUserId = UUID.randomUUID();
      String articleTitle = "기사 제목";
      String commentUserNickname = "댓글작성자";
      String commentContent = "댓글내용";
      long commentLikeCount = 3L;
      Instant now = Instant.now();
      Instant commentCreatedAt = now.minusSeconds(60);

      // when
      RecentCommentLike like = RecentCommentLike.of(
          likeId, now, commentId, articleId, articleTitle,
          commentUserId, commentUserNickname, commentContent,
          commentLikeCount, commentCreatedAt
      );

      // then
      assertThat(like.getId()).isEqualTo(likeId);
      assertThat(like.getCommentId()).isEqualTo(commentId);
      assertThat(like.getArticleId()).isEqualTo(articleId);
      assertThat(like.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(like.getCommentUserId()).isEqualTo(commentUserId);
      assertThat(like.getCommentUserNickname()).isEqualTo(commentUserNickname);
      assertThat(like.getCommentContent()).isEqualTo(commentContent);
      assertThat(like.getCommentLikeCount()).isEqualTo(commentLikeCount);
      assertThat(like.getCommentCreatedAt()).isEqualTo(commentCreatedAt);
      assertThat(like.getCreatedAt()).isEqualTo(now);
    }
  }
}
