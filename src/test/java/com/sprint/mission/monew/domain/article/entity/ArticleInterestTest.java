package com.sprint.mission.monew.domain.article.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.interest.entity.Interest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArticleInterestTest {

  @Test
  @DisplayName("create()로 생성하면 article과 interest가 올바르게 설정된다")
  void create_article과_interest가_설정된다() {
    // given
    Article article = Article.create(ArticleSource.NAVER, "https://example.com", "제목",
        Instant.now(), null);
    Interest interest = Interest.create("AI", List.of("AI"));

    // when
    ArticleInterest articleInterest = ArticleInterest.create(article, interest);

    // then
    assertThat(articleInterest.getArticle()).isEqualTo(article);
    assertThat(articleInterest.getInterest()).isEqualTo(interest);
  }
}
