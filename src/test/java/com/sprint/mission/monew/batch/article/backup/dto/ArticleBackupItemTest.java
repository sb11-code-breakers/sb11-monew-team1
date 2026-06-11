package com.sprint.mission.monew.batch.article.backup.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArticleBackupItemTest {

  @Nested
  @DisplayName("Article → ArticleBackupItem 변환")
  class From {

    @Test
    @DisplayName("Article의 모든 필드가 ArticleBackupItem에 올바르게 매핑된다")
    void Article의_모든_필드가_올바르게_매핑된다() {
      // given
      Instant publishDate = Instant.parse("2026-06-09T00:00:00Z");
      Article article = Article.create(
          ArticleSource.NAVER,
          "https://news.example.com/1",
          "테스트 기사 제목",
          publishDate,
          "기사 요약 내용"
      );

      // when
      ArticleBackupItem item = ArticleBackupItem.from(article);

      // then
      assertThat(item.id()).isEqualTo(article.getId());
      assertThat(item.source()).isEqualTo(ArticleSource.NAVER);
      assertThat(item.sourceUrl()).isEqualTo("https://news.example.com/1");
      assertThat(item.title()).isEqualTo("테스트 기사 제목");
      assertThat(item.publishDate()).isEqualTo(publishDate);
      assertThat(item.summary()).isEqualTo("기사 요약 내용");
      assertThat(item.commentCount()).isZero();
      assertThat(item.viewCount()).isZero();
    }
  }
}