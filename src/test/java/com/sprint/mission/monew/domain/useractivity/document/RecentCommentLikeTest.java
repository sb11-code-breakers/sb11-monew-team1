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
      UUID id = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID commentUserId = UUID.randomUUID();
      Instant now = Instant.now();
      Instant commentCreatedAt = now.minusSeconds(60);

      // when
      RecentCommentLike like = RecentCommentLike.of(
          id, now, commentId, articleId, "기사 제목",
          commentUserId, "댓글작성자", "댓글 내용", 3L, commentCreatedAt);

      // then
      assertThat(like.getId()).isEqualTo(id);
      assertThat(like.getCreatedAt()).isEqualTo(now);
      assertThat(like.getCommentId()).isEqualTo(commentId);
      assertThat(like.getArticleId()).isEqualTo(articleId);
      assertThat(like.getArticleTitle()).isEqualTo("기사 제목");
      assertThat(like.getCommentUserId()).isEqualTo(commentUserId);
      assertThat(like.getCommentUserNickname()).isEqualTo("댓글작성자");
      assertThat(like.getCommentContent()).isEqualTo("댓글 내용");
      assertThat(like.getCommentLikeCount()).isEqualTo(3L);
      assertThat(like.getCommentCreatedAt()).isEqualTo(commentCreatedAt);
    }
  }
}