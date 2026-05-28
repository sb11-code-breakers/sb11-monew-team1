package com.sprint.mission.monew.domain.article.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleViewTest {

  @Test
  @DisplayName("create()로 생성하면 userId와 article이 올바르게 설정된다")
  void create_userId와_article이_설정된다() {
    // given
    UUID userId = UUID.randomUUID();
    Article article = Article.create(ArticleSource.NAVER, "https://example.com", "제목",
        Instant.now(), null);

    // when
    ArticleView view = ArticleView.create(userId, article);

    // then
    assertThat(view.getUserId()).isEqualTo(userId);
    assertThat(view.getArticle()).isEqualTo(article);
  }
}
