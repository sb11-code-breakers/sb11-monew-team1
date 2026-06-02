package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ArticleUpsertServiceTest {

  @InjectMocks ArticleUpsertService articleUpsertService;
  @Mock ArticleRepository articleRepository;
  @Mock ApplicationEventPublisher eventPublisher;

  @Nested
  @DisplayName("upsert")
  class Upsert {

    @Test
    @DisplayName("신규 기사는 저장하고 ArticleCreatedEvent를 발행한다")
    void 신규_기사는_저장하고_이벤트를_발행한다() {
      // given
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "제목", Instant.now(), "요약");
      given(articleRepository.findBySourceUrl("https://example.com/1")).willReturn(Optional.empty());
      given(articleRepository.save(any(Article.class))).willReturn(saved);

      // when
      articleUpsertService.upsert(
          ArticleSource.NAVER, "https://example.com/1", "제목", Instant.now(), "요약");

      // then
      verify(articleRepository).save(any(Article.class));
      verify(eventPublisher).publishEvent(any(ArticleCreatedEvent.class));
    }

    @Test
    @DisplayName("중복 기사는 제목·요약을 업데이트하고 이벤트를 발행하지 않는다")
    void 중복_기사는_업데이트하고_이벤트를_발행하지_않는다() {
      // given
      Article existing = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");
      given(articleRepository.findBySourceUrl("https://example.com/1"))
          .willReturn(Optional.of(existing));

      // when
      articleUpsertService.upsert(
          ArticleSource.NAVER, "https://example.com/1", "수정된 제목", Instant.now(), "수정된 요약");

      // then
      verify(articleRepository).save(existing);
      verify(eventPublisher, never()).publishEvent(any());
      assertThat(existing.getTitle()).isEqualTo("수정된 제목");
      assertThat(existing.getSummary()).isEqualTo("수정된 요약");
    }

    @Test
    @DisplayName("소프트 삭제된 기사는 갱신하지 않고 건너뛴다")
    void 소프트_삭제된_기사는_건너뛴다() {
      // given
      Article deleted = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");
      deleted.softDelete();
      given(articleRepository.findBySourceUrl("https://example.com/1"))
          .willReturn(Optional.of(deleted));

      // when
      articleUpsertService.upsert(
          ArticleSource.NAVER, "https://example.com/1", "새 제목", Instant.now(), "새 요약");

      // then
      verify(articleRepository, never()).save(any());
      verify(eventPublisher, never()).publishEvent(any());
      assertThat(deleted.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    @DisplayName("sourceUrl이 null이면 건너뛴다")
    void sourceUrl이_null이면_건너뛴다() {
      // when
      articleUpsertService.upsert(ArticleSource.NAVER, null, "제목", Instant.now(), "요약");

      // then
      verify(articleRepository, never()).findBySourceUrl(any());
      verify(articleRepository, never()).save(any());
    }

    @Test
    @DisplayName("sourceUrl이 blank이면 건너뛴다")
    void sourceUrl이_blank이면_건너뛴다() {
      // when
      articleUpsertService.upsert(ArticleSource.NAVER, "  ", "제목", Instant.now(), "요약");

      // then
      verify(articleRepository, never()).findBySourceUrl(any());
      verify(articleRepository, never()).save(any());
    }
  }
}
