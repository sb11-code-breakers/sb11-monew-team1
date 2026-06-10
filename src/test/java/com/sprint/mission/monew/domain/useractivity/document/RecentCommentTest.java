package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecentCommentTest {

  @Nested
  @DisplayName("RecentComment.of()")
  class Of {

    @Test
    @DisplayName("전달한 값으로 RecentComment를 생성한다")
    void 전달한_값으로_RecentComment를_생성한다() {
      // given
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      String articleTitle = "기사 제목";
      Instant now = Instant.now();

      // when
      RecentComment comment = RecentComment.of(commentId, articleId, articleTitle, now);

      // then
      assertThat(comment.getCommentId()).isEqualTo(commentId);
      assertThat(comment.getArticleId()).isEqualTo(articleId);
      assertThat(comment.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(comment.getCreatedAt()).isEqualTo(now);
    }
  }

  @Test
  @DisplayName("RecentComment는 변동 필드를 포함하지 않아야 한다 (정확히 4개 필드)")
  void shouldOnlyContainImmutableFields() {
    // 필드 개수가 정확히 4개(commentId, articleId, articleTitle, createdAt)인지 확인
    assertThat(RecentComment.class.getDeclaredFields()).hasSize(4);
  }
}