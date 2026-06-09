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
      UUID id = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();

      // when
      RecentComment comment = RecentComment.of(
          id, articleId, "기사 제목", userId, "작성자닉네임", "댓글 내용", 5L, now);

      // then
      assertThat(comment.getId()).isEqualTo(id);
      assertThat(comment.getArticleId()).isEqualTo(articleId);
      assertThat(comment.getArticleTitle()).isEqualTo("기사 제목");
      assertThat(comment.getUserId()).isEqualTo(userId);
      assertThat(comment.getUserNickname()).isEqualTo("작성자닉네임");
      assertThat(comment.getContent()).isEqualTo("댓글 내용");
      assertThat(comment.getLikeCount()).isEqualTo(5L);
      assertThat(comment.getCreatedAt()).isEqualTo(now);
    }
  }
}