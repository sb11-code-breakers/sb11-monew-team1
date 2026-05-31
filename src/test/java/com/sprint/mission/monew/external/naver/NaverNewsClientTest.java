package com.sprint.mission.monew.external.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.naver.dto.NaverNewsResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class NaverNewsClientTest {

  @Mock RestClient restClient;
  NaverNewsClient naverNewsClient;

  @BeforeEach
  void setUp() {
    naverNewsClient = new NaverNewsClient(restClient);
    ReflectionTestUtils.setField(naverNewsClient, "clientId", "test-id");
    ReflectionTestUtils.setField(naverNewsClient, "clientSecret", "test-secret");
    ReflectionTestUtils.setField(naverNewsClient, "query", "뉴스");
    ReflectionTestUtils.setField(naverNewsClient, "display", 10);
  }

  @SuppressWarnings("unchecked")
  private void mockRestClientChain(NaverNewsResponse response) {
    RestClient.RequestHeadersUriSpec uriSpec = org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec headersSpec = org.mockito.Mockito.mock(RestClient.RequestHeadersSpec.class);
    RestClient.ResponseSpec responseSpec = org.mockito.Mockito.mock(RestClient.ResponseSpec.class);

    given(restClient.get()).willReturn(uriSpec);
    given(uriSpec.uri(anyString(), any(), any())).willReturn(headersSpec);
    given(headersSpec.header(anyString(), anyString())).willReturn(headersSpec);
    given(headersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.body(NaverNewsResponse.class)).willReturn(response);
  }

  @Nested
  @DisplayName("fetchNews")
  class FetchNews {

    @Test
    @DisplayName("정상 응답이면 기사 목록을 반환한다")
    void 정상_응답이면_기사_목록을_반환한다() {
      // given
      NaverNewsItem item = new NaverNewsItem(
          "테스트 제목", "https://example.com/1", "https://example.com/1",
          "테스트 요약", "Mon, 25 May 2026 09:00:00 +0900");
      mockRestClientChain(new NaverNewsResponse(1, 1, 1, List.of(item)));

      // when
      List<NaverNewsItem> result = naverNewsClient.fetchNews();

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).title()).isEqualTo("테스트 제목");
    }

    @Test
    @DisplayName("응답이 null이면 빈 목록을 반환한다")
    void 응답이_null이면_빈_목록을_반환한다() {
      // given
      mockRestClientChain(null);

      // when
      List<NaverNewsItem> result = naverNewsClient.fetchNews();

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("items가 null이면 빈 목록을 반환한다")
    void items가_null이면_빈_목록을_반환한다() {
      // given
      mockRestClientChain(new NaverNewsResponse(0, 1, 0, null));

      // when
      List<NaverNewsItem> result = naverNewsClient.fetchNews();

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("parseNaverDate")
  class ParseNaverDate {

    @Test
    @DisplayName("RFC 1123 형식 날짜 문자열을 Instant로 변환한다")
    void RFC_1123_날짜를_Instant로_변환한다() {
      // given — 2026-05-29는 금요일, +0900이면 UTC는 -9h → 2026-05-28T15:00:00Z
      String pubDate = "Fri, 29 May 2026 00:00:00 +0900";

      // when
      Instant result = NaverNewsClient.parseNaverDate(pubDate);

      // then
      assertThat(result).isEqualTo(Instant.parse("2026-05-28T15:00:00Z"));
    }

    @Test
    @DisplayName("파싱 불가능한 날짜는 현재 시각을 반환한다")
    void 파싱_불가능한_날짜는_현재_시각을_반환한다() {
      // given
      Instant before = Instant.now();

      // when
      Instant result = NaverNewsClient.parseNaverDate("invalid-date");

      // then
      Instant after = Instant.now();
      assertThat(result).isBetween(before, after);
    }

    @Test
    @DisplayName("null 날짜는 현재 시각을 반환한다")
    void null_날짜는_현재_시각을_반환한다() {
      // given
      Instant before = Instant.now();

      // when
      Instant result = NaverNewsClient.parseNaverDate(null);

      // then
      Instant after = Instant.now();
      assertThat(result).isBetween(before, after);
    }
  }
}
