package com.sprint.mission.monew.domain.article.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArticleTest {

  private Article article;

  @BeforeEach
  void setUp() {
    article = Article.create(
        ArticleSource.NAVER,
        "https://example.com/news/1",
        "테스트 기사 제목",
        Instant.parse("2024-01-01T00:00:00Z"),
        "기사 요약 내용"
    );
  }

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class Create {

    @Test
    @DisplayName("정상 값으로 생성하면 필드가 올바르게 설정된다")
    void 정상_값으로_생성하면_필드가_올바르게_설정된다() {
      // then
      assertThat(article.getId()).isNotNull();
      assertThat(article.getSource()).isEqualTo(ArticleSource.NAVER);
      assertThat(article.getSourceUrl()).isEqualTo("https://example.com/news/1");
      assertThat(article.getTitle()).isEqualTo("테스트 기사 제목");
      assertThat(article.getPublishDate()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
      assertThat(article.getSummary()).isEqualTo("기사 요약 내용");
    }

    @Test
    @DisplayName("기본 commentCount와 viewCount는 0이다")
    void 기본_commentCount와_viewCount는_0이다() {
      // then
      assertThat(article.getCommentCount()).isZero();
      assertThat(article.getViewCount()).isZero();
    }

    @Test
    @DisplayName("생성 직후 deletedAt은 null이다")
    void 생성_직후_deletedAt은_null이다() {
      // then
      assertThat(article.getDeletedAt()).isNull();
      assertThat(article.isDeleted()).isFalse();
    }
  }


  @Nested
  @DisplayName("소프트 딜리트")
  class SoftDelete {

    @Test
    @DisplayName("softDelete 호출 후 deletedAt이 설정된다")
    void softDelete_호출_후_deletedAt이_설정된다() {
      // when
      article.softDelete();

      // then
      assertThat(article.getDeletedAt()).isNotNull();
      assertThat(article.isDeleted()).isTrue();
    }
  }

  @Nested
  @DisplayName("제목·요약 업데이트")
  class Update {

    @Test
    @DisplayName("update 호출 시 제목과 요약이 변경된다")
    void update_호출_시_제목과_요약이_변경된다() {
      // when
      article.update("새 제목", "새 요약");

      // then
      assertThat(article.getTitle()).isEqualTo("새 제목");
      assertThat(article.getSummary()).isEqualTo("새 요약");
    }

    @Test
    @DisplayName("update 호출 시 다른 필드는 변경되지 않는다")
    void update_호출_시_다른_필드는_변경되지_않는다() {
      // when
      article.update("새 제목", "새 요약");

      // then
      assertThat(article.getSource()).isEqualTo(ArticleSource.NAVER);
      assertThat(article.getSourceUrl()).isEqualTo("https://example.com/news/1");
      assertThat(article.getViewCount()).isZero();
    }
  }
}
