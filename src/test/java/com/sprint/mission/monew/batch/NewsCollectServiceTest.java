package com.sprint.mission.monew.batch;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
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
class NewsCollectServiceTest {

  @InjectMocks NewsCollectService newsCollectService;
  @Mock ArticleUpsertService articleUpsertService;
  @Mock NaverNewsClient naverNewsClient;
  @Mock RssNewsParser rssNewsParser;
  @Mock NewsCollectMetrics newsCollectMetrics;

  @Nested
  @DisplayName("뉴스 수집")
  class Collect {

    @Test
    @DisplayName("Naver 기사를 수집하면 ArticleUpsertService에 위임한다")
    void Naver_기사를_수집하면_ArticleUpsertService에_위임한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsert(
          eq(ArticleSource.NAVER), eq("https://example.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("RSS 기사를 수집하면 ArticleUpsertService에 위임한다")
    void RSS_기사를_수집하면_ArticleUpsertService에_위임한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsert(
          eq(ArticleSource.HANKYUNG), eq("https://hankyung.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("기사 단건 처리 실패 시 출처별 실패 건수를 집계한다")
    void 기사_단건_처리_실패_시_출처별_실패_건수를_집계한다() {
      // given — Naver 기사 단건 upsert 중 예외 발생
      NaverNewsItem item = new NaverNewsItem(
          "테스트 기사", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());
      willThrow(new RuntimeException("저장 실패"))
          .given(articleUpsertService)
          .upsert(eq(ArticleSource.NAVER), eq("https://example.com/1"), any(), any(), any());

      // when — 단건 실패는 삼켜지고 수집은 계속된다
      newsCollectService.collect();

      // then — 실패한 단건은 출처별 실패 건수로 집계된다
      verify(newsCollectMetrics).countFailed(ArticleSource.NAVER);
    }

    @Test
    @DisplayName("출처별 수집 건수를 집계한다")
    void 출처별_수집_건수를_집계한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then — Naver 1건 수집이 출처별 건수로 집계된다
      verify(newsCollectMetrics).countCollected(ArticleSource.NAVER, 1);
    }

    @Test
    @DisplayName("한 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void 한_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willThrow(new RuntimeException("Naver API 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when & then — 예외 없이 완료, HANKYUNG은 upsert 호출됨
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsert(
          eq(ArticleSource.HANKYUNG), eq("https://hankyung.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("RSS 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void RSS_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto chosunItem = new RssArticleDto(
          ArticleSource.CHOSUN, "https://chosun.com/1", "조선 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willThrow(new RuntimeException("RSS 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of(chosunItem));
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when & then
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsert(
          eq(ArticleSource.CHOSUN), eq("https://chosun.com/1"), any(), any(), any());
    }

    @Test
    @DisplayName("originallink·link 모두 null인 Naver 기사는 null sourceUrl로 upsert를 호출한다")
    void originallink_link_모두_null인_기사는_null_sourceUrl로_upsert를_호출한다() {
      // given — originallink, link 모두 null → sourceUrl = null (skip은 ArticleUpsertService 내부 처리)
      NaverNewsItem item = new NaverNewsItem("제목", null, null, "요약",
          "Mon, 29 May 2026 00:00:00 +0900");
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService).upsert(eq(ArticleSource.NAVER), eq(null), any(), any(), any());
    }

    @Test
    @DisplayName("Naver 기사 단건 처리 실패 시 같은 출처의 다른 기사는 계속 처리한다")
    void Naver_기사_단건_처리_실패_시_다른_기사는_계속_처리한다() {
      // given
      NaverNewsItem item1 = new NaverNewsItem(
          "제목1", "https://example.com/1", "https://example.com/1",
          "요약1", "Mon, 29 May 2026 00:00:00 +0900");
      NaverNewsItem item2 = new NaverNewsItem(
          "제목2", "https://example.com/2", "https://example.com/2",
          "요약2", "Mon, 29 May 2026 00:00:00 +0900");

      given(naverNewsClient.fetchNews()).willReturn(List.of(item1, item2));
      given(rssNewsParser.parse(any())).willReturn(List.of());
      willThrow(new RuntimeException("저장 실패"))
          .given(articleUpsertService).upsert(eq(ArticleSource.NAVER), eq("https://example.com/1"), any(), any(), any());

      // when & then — 예외 없이 완료, 두 번째 기사도 upsert 호출됨
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsert(
          eq(ArticleSource.NAVER), eq("https://example.com/2"), any(), any(), any());
    }

    @Test
    @DisplayName("pubDate가 null인 Naver 기사는 upsert를 호출하지 않고 건너뛴다")
    void pubDate가_null인_Naver_기사는_건너뛴다() {
      // given — pubDate null → parseNaverDate() → Optional.empty() → skip
      NaverNewsItem item = new NaverNewsItem("제목", "https://example.com/1", "https://example.com/1",
          "요약", null);
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      newsCollectService.collect();

      // then
      verify(articleUpsertService, never()).upsert(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("RSS 기사 단건 처리 실패 시 같은 출처의 다른 기사는 계속 처리한다")
    void RSS_기사_단건_처리_실패_시_다른_기사는_계속_처리한다() {
      // given
      RssArticleDto item1 = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "기사1", Instant.now(), "요약1");
      RssArticleDto item2 = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/2", "기사2", Instant.now(), "요약2");

      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(item1, item2));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());
      willThrow(new RuntimeException("저장 실패"))
          .given(articleUpsertService).upsert(eq(ArticleSource.HANKYUNG), eq("https://hankyung.com/1"), any(), any(), any());

      // when & then — 두 번째 기사도 upsert 호출됨
      assertThatNoException().isThrownBy(() -> newsCollectService.collect());
      verify(articleUpsertService).upsert(
          eq(ArticleSource.HANKYUNG), eq("https://hankyung.com/2"), any(), any(), any());
    }
  }
}
