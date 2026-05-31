package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.Article;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.article.event.ArticleCreatedEvent;
import com.sprint.mission.monew.domain.article.repository.ArticleRepository;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.util.List;
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
class NewsCollectServiceTest {

  @InjectMocks NewsCollectService newsCollectService;
  @Mock ArticleRepository articleRepository;
  @Mock NaverNewsClient naverNewsClient;
  @Mock RssNewsParser rssNewsParser;
  @Mock ApplicationEventPublisher eventPublisher;

  @Nested
  @DisplayName("뉴스 수집")
  class Collect {

    @Test
    @DisplayName("신규 기사는 저장하고 ArticleCreatedEvent를 발행한다")
    void 신규_기사는_저장하고_이벤트를_발행한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "테스트 기사", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      Article saved = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "테스트 기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());
      given(articleRepository.findBySourceUrl("https://example.com/1")).willReturn(Optional.empty());
      given(articleRepository.save(any(Article.class))).willReturn(saved);

      // when
      newsCollectService.collect();

      // then
      verify(articleRepository).save(any(Article.class));
      verify(eventPublisher).publishEvent(any(ArticleCreatedEvent.class));
    }

    @Test
    @DisplayName("중복 기사는 제목·요약을 업데이트하고 이벤트를 발행하지 않는다")
    void 중복_기사는_업데이트하고_이벤트를_발행하지_않는다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "수정된 제목", "https://example.com/1", "https://example.com/1",
          "수정된 요약", "Mon, 29 May 2026 00:00:00 +0900");
      Article existing = Article.create(
          ArticleSource.NAVER, "https://example.com/1", "원래 제목", Instant.now(), "원래 요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());
      given(articleRepository.findBySourceUrl("https://example.com/1"))
          .willReturn(Optional.of(existing));

      // when
      newsCollectService.collect();

      // then — update 케이스: 기존 기사에 대해 save() 호출, 이벤트는 발행 안 함
      verify(articleRepository).save(existing);
      verify(eventPublisher, never()).publishEvent(any());
      assertThat(existing.getTitle()).isEqualTo("수정된 제목");
      assertThat(existing.getSummary()).isEqualTo("수정된 요약");
    }

    @Test
    @DisplayName("RSS 기사도 신규이면 저장하고 이벤트를 발행한다")
    void RSS_신규_기사는_저장하고_이벤트를_발행한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      Article saved = Article.create(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());
      given(articleRepository.findBySourceUrl("https://hankyung.com/1")).willReturn(Optional.empty());
      given(articleRepository.save(any(Article.class))).willReturn(saved);

      // when
      newsCollectService.collect();

      // then
      verify(articleRepository).save(any(Article.class));
      verify(eventPublisher).publishEvent(any(ArticleCreatedEvent.class));
    }

    @Test
    @DisplayName("RSS 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void RSS_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.CHOSUN, "https://chosun.com/1", "조선 기사", Instant.now(), "요약");
      Article saved = Article.create(
          ArticleSource.CHOSUN, "https://chosun.com/1", "조선 기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willThrow(new RuntimeException("RSS 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());
      given(articleRepository.findBySourceUrl("https://chosun.com/1")).willReturn(Optional.empty());
      given(articleRepository.save(any(Article.class))).willReturn(saved);

      // when & then — 예외 없이 완료, CHOSUN 기사는 저장
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleRepository).save(any(Article.class));
    }

    @Test
    @DisplayName("sourceUrl이 null인 기사는 저장하지 않는다")
    void sourceUrl이_null인_기사는_저장하지_않는다() {
      // given — originallink, link 모두 null → sourceUrl = null
      NaverNewsItem item = new NaverNewsItem("제목", null, null, "요약",
          "Mon, 25 May 2026 09:00:00 +0900");

      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleRepository, never()).save(any());
      verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("한 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void 한_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      Article saved = Article.create(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willThrow(new RuntimeException("Naver API 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());
      given(articleRepository.findBySourceUrl("https://hankyung.com/1")).willReturn(Optional.empty());
      given(articleRepository.save(any(Article.class))).willReturn(saved);

      // when & then — 예외 없이 완료
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleRepository).save(any(Article.class));
    }
  }
}
