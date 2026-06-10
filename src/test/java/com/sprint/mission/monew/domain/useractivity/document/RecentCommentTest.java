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
      String articleTitle = "자바 백엔드 개발자의 길";
      Instant now = Instant.now();

      // when
      // 💡 다이어트 파라미터 적용
      RecentComment comment = RecentComment.of(commentId, articleId, articleTitle, now);

      // then
      assertThat(comment.getCommentId()).isEqualTo(commentId);
      assertThat(comment.getArticleId()).isEqualTo(articleId);
      assertThat(comment.getArticleTitle()).isEqualTo(articleTitle);

      // ⚠️ 만약 엔티티 변수명이 writtenAt 등이라면 getWrittenAt()으로 변경하세요.
      assertThat(comment.getCreatedAt()).isEqualTo(now);
    }
  }
}