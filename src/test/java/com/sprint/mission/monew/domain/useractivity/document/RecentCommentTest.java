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
      UUID userId = UUID.randomUUID();
      String articleTitle = "기사 제목";
      String userNickname = "작성자";
      String content = "댓글 내용";
      Instant now = Instant.now();

      // when
      RecentComment comment = RecentComment.of(commentId, articleId, articleTitle, userId, userNickname, content, 0L, now);

      // then
      assertThat(comment.getId()).isEqualTo(commentId);
      assertThat(comment.getArticleId()).isEqualTo(articleId);
      assertThat(comment.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(comment.getUserId()).isEqualTo(userId);
      assertThat(comment.getUserNickname()).isEqualTo(userNickname);
      assertThat(comment.getContent()).isEqualTo(content);
      assertThat(comment.getLikeCount()).isZero();
      assertThat(comment.getCreatedAt()).isEqualTo(now);
    }
  }
}
