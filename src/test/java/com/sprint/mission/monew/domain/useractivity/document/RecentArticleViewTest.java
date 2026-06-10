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
      UUID articleId = UUID.randomUUID();
      Instant now = Instant.now();
      Instant publishedDate = now.minusSeconds(3600);

      // when
      RecentArticleView view = RecentArticleView.of(
          articleId, "NAVER", "https://news.naver.com/article/1",
          "스프링 부트 하이브리드 아키텍처", publishedDate, "기사 요약", now);

      // then
      assertThat(view.getArticleId()).isEqualTo(articleId);
      assertThat(view.getSource()).isEqualTo("NAVER");
      assertThat(view.getSourceUrl()).isEqualTo("https://news.naver.com/article/1");
      assertThat(view.getArticleTitle()).isEqualTo("스프링 부트 하이브리드 아키텍처");
      assertThat(view.getArticlePublishedDate()).isEqualTo(publishedDate);
      assertThat(view.getArticleSummary()).isEqualTo("기사 요약");
      assertThat(view.getViewedAt()).isEqualTo(now);
    }
  }

  @Test
  @DisplayName("RecentArticleView는 변동 필드를 포함하지 않아야 한다 (정확히 7개 필드)")
  void shouldOnlyContainImmutableFields() {
    // 필드 개수가 정확히 7개인지 확인하여 누군가 실수로 필드를 추가하는 것을 방지
    assertThat(RecentArticleView.class.getDeclaredFields()).hasSize(7);
  }
}