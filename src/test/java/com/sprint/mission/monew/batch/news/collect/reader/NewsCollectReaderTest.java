package com.sprint.mission.monew.batch.news.collect.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.batch.news.collect.dto.NewsCollectItem;
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
public class NewsCollectReaderTest {

  @Mock
  private NaverNewsClient naverNewsClient;

  @Mock
  private RssNewsParser rssNewsParser;

  @Mock
  private NewsCollectMetrics newsCollectMetrics;

  @InjectMocks
  private NewsCollectReader reader;

  @Nested
  @DisplayName("뉴스 기사 읽기")
  class Reader {

    @Test
    @DisplayName("네이버 기사 조회 성공")
    void 기사_조회_네이버() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1",
          "요약", "Mon, 29 May 2026 00:00:00 +0900");

      when(naverNewsClient.fetchNews()).thenReturn(List.of(item));
      when(rssNewsParser.parse(any())).thenReturn(List.of());

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.NAVER);

      verify(newsCollectMetrics).countCollected(ArticleSource.NAVER, 1);
    }

    @Test
    @DisplayName("RSS 기사 조회 성공")
    void 기사_조회_RSS() {
      // given
      // HANKYUNG 기사만 있음
      RssArticleDto item = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");

      when(naverNewsClient.fetchNews()).thenReturn(List.of());
      when(rssNewsParser.parse(ArticleSource.HANKYUNG)).thenReturn(List.of(item));
      when(rssNewsParser.parse(ArticleSource.CHOSUN)).thenReturn(List.of());
      when(rssNewsParser.parse(ArticleSource.YONHAP)).thenReturn(List.of());

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.HANKYUNG);

      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 1);
    }

    @Test
    @DisplayName("Naver 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void _Naver_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(naverNewsClient.fetchNews()).willThrow(new RuntimeException("Naver API 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(rssItem));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when & then — 예외 없이 완료, HANKYUNG은 upsert 호출됨
      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.HANKYUNG);

      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 1);
    }

    @Test
    @DisplayName("RSS 출처 수집 실패 시 다른 출처는 계속 읽는다.")
    void _RSS_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto chosunItem = new RssArticleDto(
          ArticleSource.CHOSUN, "https://chosun.com/1", "조선 기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willThrow(
          new RuntimeException("RSS 오류"));
      given(rssNewsParser.parse(eq(ArticleSource.CHOSUN))).willReturn(List.of(chosunItem));
      given(rssNewsParser.parse(eq(ArticleSource.YONHAP))).willReturn(List.of());

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.CHOSUN);

      verify(newsCollectMetrics).countCollected(ArticleSource.CHOSUN, 1);
    }

    @Test
    @DisplayName("originallink·link 모두 null인 Naver 기사는 Reader에서 생성되지 않고 건너뛴다")
    void originallink_link_모두_null인_기사는_건너뛴다() {

      // given
      NaverNewsItem item = new NaverNewsItem(
          "제목",
          null,
          null,
          "요약",
          "Mon, 29 May 2026 00:00:00 +0900"
      );

      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNull();
      verify(newsCollectMetrics).countCollected(ArticleSource.NAVER, 0);
    }

    @Test
    @DisplayName("pubDate가 null인 Naver 기사는 건너뛴다")
    void pubDate가_null인_Naver_기사는_건너뛴다() {
      // given — pubDate null → parseNaverDate() → Optional.empty() → skip
      NaverNewsItem item = new NaverNewsItem("제목", "https://example.com/1", "https://example.com/1",
          "요약", null);
      given(naverNewsClient.fetchNews()).willReturn(List.of(item));
      given(rssNewsParser.parse(any())).willReturn(List.of());

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNull();
      verify(newsCollectMetrics).countCollected(ArticleSource.NAVER, 0);
    }

    @Test
    @DisplayName("RSS sourceUrl이 null이면 NewsCollectItem이 생성되지 않는다")
    void RSS_sourceUrl_null_이면_아이템이_생성되지_않는다() {

      RssArticleDto item = new RssArticleDto(
          ArticleSource.HANKYUNG, null, "기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(item));

      NewsCollectItem result = reader.read();

      assertThat(result).isNull();
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 0);
    }

    @Test
    @DisplayName("RSS sourceUrl이 blank이면 NewsCollectItem이 생성되지 않는다")
    void RSS_sourceUrl_blank_이면_아이템이_생성되지_않는다() {

      RssArticleDto item = new RssArticleDto(
          ArticleSource.HANKYUNG, "   ", "기사", Instant.now(), "요약");

      given(naverNewsClient.fetchNews()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(item));

      NewsCollectItem result = reader.read();

      assertThat(result).isNull();
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 0);
    }
  }
}
