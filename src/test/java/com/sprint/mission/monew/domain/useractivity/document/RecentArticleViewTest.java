package com.sprint.mission.monew.domain.useractivity.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecentArticleViewTest {

  @Nested
  @DisplayName("RecentArticleView.of()")
  class Of {

    @Test
    @DisplayName("전달한 값으로 RecentArticleView를 생성한다")
    void 전달한_값으로_RecentArticleView를_생성한다() {
      // given
      UUID id = UUID.randomUUID();
      UUID viewedBy = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      Instant now = Instant.now();
      Instant publishedDate = now.minusSeconds(3600);

      // when
      RecentArticleView view = RecentArticleView.of(
          id, viewedBy, now, articleId,
          "NAVER", "https://news.naver.com/article/1",
          "기사 제목", publishedDate, "기사 요약", 10L, 200L);

      // then
      assertThat(view.getId()).isEqualTo(id);
      assertThat(view.getViewedBy()).isEqualTo(viewedBy);
      assertThat(view.getCreatedAt()).isEqualTo(now);
      assertThat(view.getArticleId()).isEqualTo(articleId);
      assertThat(view.getSource()).isEqualTo("NAVER");
      assertThat(view.getSourceUrl()).isEqualTo("https://news.naver.com/article/1");
      assertThat(view.getArticleTitle()).isEqualTo("기사 제목");
      assertThat(view.getArticlePublishedDate()).isEqualTo(publishedDate);
      assertThat(view.getArticleSummary()).isEqualTo("기사 요약");
      assertThat(view.getArticleCommentCount()).isEqualTo(10L);
      assertThat(view.getArticleViewCount()).isEqualTo(200L);
    }
  }
}