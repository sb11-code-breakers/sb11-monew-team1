
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
        UUID commentId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        String articleTitle = "기사 제목";
        Instant now = Instant.now();
        Instant commentCreatedAt = now.minusSeconds(60);

        // when
        RecentCommentLike like = RecentCommentLike.of(
            commentId, articleId, articleTitle, commentCreatedAt, now
        );

        // then
        assertThat(like.getCommentId()).isEqualTo(commentId);
        assertThat(like.getArticleId()).isEqualTo(articleId);
        assertThat(like.getArticleTitle()).isEqualTo(articleTitle);
        assertThat(like.getCommentCreatedAt()).isEqualTo(commentCreatedAt);
        assertThat(like.getLikedAt()).isEqualTo(now);
      }
    }
    @Test
    @DisplayName("RecentCommentLike는 변동 필드를 포함하지 않아야 한다")
    void shouldOnlyContainImmutableFields() {
      // 필드 개수가 5개인지 확인
      assertThat(RecentCommentLike.class.getDeclaredFields())
          .hasSize(5);
    }
  }