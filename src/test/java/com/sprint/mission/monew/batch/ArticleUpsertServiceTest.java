package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleUpsertServiceTest {

  @InjectMocks ArticleUpsertService articleUpsertService;
  @Mock ArticleRepository articleRepository;
  @Mock NewsCollectMetrics newsCollectMetrics;

  @Nested
  @DisplayName("upsertAll")
  class UpsertAll {

    @Test
    @DisplayName("신규 기사는 일괄 저장한다")
    void 신규_기사는_일괄_저장한다() {
      // given
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "제목", Instant.now(), "요약");
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "제목", Instant.now(), "요약");
      given(articleRepository.findBySourceUrlIn(List.of("https://example.com/1")))
          .willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willReturn(List.of(saved));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository).saveAll(anyList());
      verify(newsCollectMetrics).countCreated();
    }

    @Test
    @DisplayName("기존 기사는 제목·요약을 업데이트하고 saveAll을 호출하지 않는다")
    void 기존_기사는_업데이트하고_saveAll을_호출하지_않는다() {
      // given
      Article existing = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "수정된 제목", Instant.now(), "수정된 요약");
      given(articleRepository.findBySourceUrlIn(List.of("https://example.com/1")))
          .willReturn(List.of(existing));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then — 기존 기사만 있으면 saveAll 호출 없이 dirty-checking으로 업데이트
      verify(articleRepository, never()).saveAll(any());
      verify(newsCollectMetrics).countDuplicated();
      assertThat(existing.getTitle()).isEqualTo("수정된 제목");
      assertThat(existing.getSummary()).isEqualTo("수정된 요약");
    }

    @Test
    @DisplayName("소프트 삭제된 기사는 업데이트하지 않고 건너뛴다")
    void 소프트_삭제된_기사는_건너뛴다() {
      // given
      Article deleted = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");
      deleted.softDelete();
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "새 제목", Instant.now(), "새 요약");
      given(articleRepository.findBySourceUrlIn(List.of("https://example.com/1")))
          .willReturn(List.of(deleted));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then
      verify(articleRepository, never()).saveAll(any());
      verify(newsCollectMetrics, never()).countDuplicated();
      assertThat(deleted.getTitle()).isEqualTo("원래 제목");
    }

    @Test
    @DisplayName("candidates가 비어있으면 DB 조회를 하지 않는다")
    void candidates가_비어있으면_DB_조회를_하지_않는다() {
      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of());

      // then
      verify(articleRepository, never()).findBySourceUrlIn(any());
      verify(articleRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("null sourceUrl candidates는 필터링하고 DB 조회를 하지 않는다")
    void null_sourceUrl_candidates는_필터링한다() {
      // given — null sourceUrl → dedup 필터에서 제거 (line 34 null 분기)
      ArticleCandidate nullUrl = new ArticleCandidate(null, "제목", Instant.now(), "요약");

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(nullUrl));

      // then
      verify(articleRepository, never()).findBySourceUrlIn(any());
      verify(articleRepository, never()).saveAll(any());
      verify(newsCollectMetrics, never()).countCreated();
      verify(newsCollectMetrics, never()).countDuplicated();
    }

    @Test
    @DisplayName("blank sourceUrl candidates는 필터링하고 DB 조회를 하지 않는다")
    void blank_sourceUrl_candidates는_필터링한다() {
      // given — blank sourceUrl → dedup 필터에서 제거 (line 34 blank 분기)
      ArticleCandidate blank = new ArticleCandidate("  ", "제목", Instant.now(), "요약");

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(blank));

      // then
      verify(articleRepository, never()).findBySourceUrlIn(any());
      verify(articleRepository, never()).saveAll(any());
      verify(newsCollectMetrics, never()).countCreated();
      verify(newsCollectMetrics, never()).countDuplicated();
    }

    @Test
    @DisplayName("동일 sourceUrl 중복 candidates는 first-seen 하나만 저장한다")
    void 동일_sourceUrl_중복_candidates는_하나만_저장한다() {
      // given — 같은 URL 2건 → dedup 후 1건만 saveAll
      ArticleCandidate first = new ArticleCandidate(
          "https://example.com/1", "첫 번째 제목", Instant.now(), "첫 번째 요약");
      ArticleCandidate second = new ArticleCandidate(
          "https://example.com/1", "두 번째 제목", Instant.now(), "두 번째 요약");
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "첫 번째 제목", Instant.now(), "첫 번째 요약");
      given(articleRepository.findBySourceUrlIn(List.of("https://example.com/1")))
          .willReturn(List.of());
      given(articleRepository.saveAll(anyList())).willReturn(List.of(saved));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(first, second));

      // then — saveAll은 1건만(first-seen 기준), title·summary도 첫 번째 candidate 값
      verify(articleRepository).saveAll(argThat((List<Article> list) ->
          list.size() == 1
              && list.get(0).getSourceUrl().equals("https://example.com/1")
              && list.get(0).getTitle().equals("첫 번째 제목")
              && list.get(0).getSummary().equals("첫 번째 요약")));
      verify(newsCollectMetrics).countCreated();
    }

    @Test
    @DisplayName("findBySourceUrlIn이 중복 결과를 반환하면 first-seen 엔티티를 업데이트한다")
    void findBySourceUrlIn_중복_결과는_first_seen_엔티티를_업데이트한다() {
      // given — 같은 URL로 두 건 반환 → mergeFunction (a, b) -> a 로 article1 채택
      Article article1 = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "제목1", Instant.now(), "요약1");
      Article article2 = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "제목2", Instant.now(), "요약2");
      ArticleCandidate candidate = new ArticleCandidate(
          "https://example.com/1", "새 제목", Instant.now(), "새 요약");
      given(articleRepository.findBySourceUrlIn(anyList())).willReturn(List.of(article1, article2));

      // when
      articleUpsertService.upsertAll(ArticleSource.NAVER, List.of(candidate));

      // then — article1(first-seen)만 업데이트되고, article2는 변경 없음
      assertThat(article1.getTitle()).isEqualTo("새 제목");
      assertThat(article1.getSummary()).isEqualTo("새 요약");
      assertThat(article2.getTitle()).isEqualTo("제목2");
      assertThat(article2.getSummary()).isEqualTo("요약2");
      verify(newsCollectMetrics).countDuplicated();
      verify(articleRepository, never()).saveAll(any());
    }
  }
}
