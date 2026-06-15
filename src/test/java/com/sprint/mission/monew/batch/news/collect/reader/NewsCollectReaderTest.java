package com.sprint.mission.monew.batch.news.collect.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.batch.news.collect.dto.NewsCollectItem;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NewsCollectReaderTest {

  @Mock private NaverNewsClient naverNewsClient;
  @Mock private RssNewsParser rssNewsParser;
  @Mock private InterestRepository interestRepository;
  @Mock private NewsCollectMetrics newsCollectMetrics;

  @InjectMocks
  private NewsCollectReader reader;

  private NaverNewsItem recentNaverItem() {
    // pubDate: 현재 시각 기준 30분 전 → cutoff(1시간 전) 이후이므로 다음 페이지 요청 조건
    String pubDate = "Mon, 29 May 2026 00:00:00 +0900";
    return new NaverNewsItem(
        "제목", "https://example.com/1", "https://example.com/1", "요약", pubDate);
  }

  private NaverNewsItem oldNaverItem() {
    // pubDate: 2시간 전 → cutoff 이전이므로 페이지네이션 중단 조건
    String pubDate = "Mon, 28 Apr 2024 00:00:00 +0900";
    return new NaverNewsItem(
        "오래된 기사", "https://example.com/old", "https://example.com/old", "요약", pubDate);
  }

  private Interest interestWithKeyword(String keyword) {
    return Interest.create(keyword, 0, List.of(keyword));
  }

  @BeforeEach
  void setUp() {
    given(rssNewsParser.parse(any())).willReturn(List.of());
  }

  @Nested
  @DisplayName("Naver 수집")
  class NaverCollect {

    @Test
    @DisplayName("키워드별로 fetchNews를 호출하고 기사를 수집한다")
    void 키워드별로_fetchNews를_호출하고_기사를_수집한다() {
      // given
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(interestWithKeyword("AI")));
      given(naverNewsClient.fetchNews(eq("AI"), anyInt()))
          .willReturn(List.of(oldNaverItem()));

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.NAVER);
    }

    @Test
    @DisplayName("등록된 관심사 키워드가 없으면 Naver 기사를 수집하지 않는다")
    void 관심사_키워드_없으면_Naver_수집_안_한다() {
      // given
      given(interestRepository.findAllWithKeywords()).willReturn(List.of());

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("마지막 기사 pubDate가 cutoff 이전이면 다음 페이지를 요청하지 않는다")
    void 오래된_기사면_다음_페이지_요청_안_한다() {
      // given
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(interestWithKeyword("AI")));
      given(naverNewsClient.fetchNews(eq("AI"), eq(1)))
          .willReturn(List.of(oldNaverItem()));

      // when
      reader.read();

      // then — page=1 한 번만 호출되고 page=2는 호출되지 않음
      verify(naverNewsClient).fetchNews("AI", 1);
      verify(naverNewsClient, never()).fetchNews("AI", 2);
    }

    @Test
    @DisplayName("여러 키워드는 중복 제거 후 각각 독립 호출한다")
    void 중복_키워드는_제거_후_각각_호출한다() {
      // given — 두 관심사가 "AI" 키워드를 공유
      given(interestRepository.findAllWithKeywords()).willReturn(
          List.of(interestWithKeyword("AI"), interestWithKeyword("AI")));
      given(naverNewsClient.fetchNews(eq("AI"), anyInt()))
          .willReturn(List.of(oldNaverItem()));

      // when
      reader.read();

      // then — 중복 제거로 page=1 한 번만 호출
      verify(naverNewsClient).fetchNews("AI", 1);
    }

    @Test
    @DisplayName("Naver API 호출 실패 시 해당 키워드를 건너뛰고 RSS 수집을 계속한다")
    void API_실패_시_해당_키워드_건너뛰고_계속한다() {
      // given
      RssArticleDto rssItem = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(interestWithKeyword("AI")));
      given(naverNewsClient.fetchNews(anyString(), anyInt()))
          .willThrow(new RuntimeException("Naver API 오류"));
      given(rssNewsParser.parse(ArticleSource.HANKYUNG)).willReturn(List.of(rssItem));

      // when
      NewsCollectItem result = reader.read();

      // then — Naver 실패해도 RSS 기사 반환
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.HANKYUNG);
      verify(newsCollectMetrics).countFailed(ArticleSource.NAVER);
    }

    @Test
    @DisplayName("originallink·link 모두 null인 Naver 기사는 건너뛴다")
    void originallink_link_모두_null인_기사는_건너뛴다() {
      // given
      NaverNewsItem nullUrlItem = new NaverNewsItem(
          "제목", null, null, "요약", "Mon, 29 May 2026 00:00:00 +0900");
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(interestWithKeyword("AI")));
      given(naverNewsClient.fetchNews(anyString(), anyInt()))
          .willReturn(List.of(nullUrlItem));

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("pubDate가 null인 Naver 기사는 건너뛴다")
    void pubDate가_null인_Naver_기사는_건너뛴다() {
      // given
      NaverNewsItem nullDateItem = new NaverNewsItem(
          "제목", "https://example.com/1", "https://example.com/1", "요약", null);
      given(interestRepository.findAllWithKeywords())
          .willReturn(List.of(interestWithKeyword("AI")));
      given(naverNewsClient.fetchNews(anyString(), anyInt()))
          .willReturn(List.of(nullDateItem));

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("RSS 수집")
  class RssCollect {

    @Test
    @DisplayName("RSS 기사 조회 성공")
    void 기사_조회_RSS() {
      // given
      RssArticleDto item = new RssArticleDto(
          ArticleSource.HANKYUNG, "https://hankyung.com/1", "한경 기사", Instant.now(), "요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of());
      given(rssNewsParser.parse(ArticleSource.HANKYUNG)).willReturn(List.of(item));

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNotNull();
      assertThat(result.source()).isEqualTo(ArticleSource.HANKYUNG);
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 1);
    }

    @Test
    @DisplayName("RSS 출처 수집 실패 시 다른 출처는 계속 수집한다")
    void RSS_출처_실패_시_다른_출처는_계속_수집한다() {
      // given
      RssArticleDto chosunItem = new RssArticleDto(
          ArticleSource.CHOSUN, "https://chosun.com/1", "조선 기사", Instant.now(), "요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG)))
          .willThrow(new RuntimeException("RSS 오류"));
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
    @DisplayName("RSS sourceUrl이 null이면 NewsCollectItem이 생성되지 않는다")
    void RSS_sourceUrl_null_이면_아이템이_생성되지_않는다() {
      // given
      RssArticleDto item = new RssArticleDto(
          ArticleSource.HANKYUNG, null, "기사", Instant.now(), "요약");
      given(interestRepository.findAllWithKeywords()).willReturn(List.of());
      given(rssNewsParser.parse(eq(ArticleSource.HANKYUNG))).willReturn(List.of(item));

      // when
      NewsCollectItem result = reader.read();

      // then
      assertThat(result).isNull();
      verify(newsCollectMetrics).countCollected(ArticleSource.HANKYUNG, 0);
    }
  }
}
