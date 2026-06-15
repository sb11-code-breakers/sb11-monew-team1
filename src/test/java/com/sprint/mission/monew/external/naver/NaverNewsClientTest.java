package com.sprint.mission.monew.external.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.naver.dto.NaverNewsResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    ReflectionTestUtils.setField(naverNewsClient, "display", 10);
  }

  @SuppressWarnings("unchecked")
  private RestClient.RequestHeadersUriSpec mockRestClientChain(NaverNewsResponse response) {
    RestClient.RequestHeadersUriSpec uriSpec = org.mockito.Mockito.mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec headersSpec = org.mockito.Mockito.mock(RestClient.RequestHeadersSpec.class);
    RestClient.ResponseSpec responseSpec = org.mockito.Mockito.mock(RestClient.ResponseSpec.class);

    given(restClient.get()).willReturn(uriSpec);
    given(uriSpec.uri(anyString(), any(), any(), any())).willReturn(headersSpec);
    given(headersSpec.header(anyString(), anyString())).willReturn(headersSpec);
    given(headersSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.body(NaverNewsResponse.class)).willReturn(response);
    return uriSpec;
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
      List<NaverNewsItem> result = naverNewsClient.fetchNews("AI", 1);

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
      List<NaverNewsItem> result = naverNewsClient.fetchNews("AI", 1);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("items가 null이면 빈 목록을 반환한다")
    void items가_null이면_빈_목록을_반환한다() {
      // given
      mockRestClientChain(new NaverNewsResponse(0, 1, 0, null));

      // when
      List<NaverNewsItem> result = naverNewsClient.fetchNews("AI", 1);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("page=2이면 start=display+1로 계산된다")
    void page2이면_start가_올바르게_계산된다() {
      // given — display=10이므로 page=2 → start=11
      RestClient.RequestHeadersUriSpec uriSpec =
          mockRestClientChain(new NaverNewsResponse(0, 11, 0, List.of()));

      // when
      List<NaverNewsItem> result = naverNewsClient.fetchNews("AI", 2);

      // then — URI가 start=11로 호출됐는지 검증
      assertThat(result).isEmpty();
      verify(uriSpec).uri(anyString(), eq("AI"), eq(10), eq(11));
    }
  }

  @Nested
  @DisplayName("parseNaverDate")
  class ParseNaverDate {

    @Test
    @DisplayName("정상 날짜는 Optional에 담긴 Instant를 반환한다")
    void 정상_날짜는_Optional에_담긴_Instant를_반환한다() {
      // given — 2026-05-29는 금요일, +0900이면 UTC는 -9h → 2026-05-28T15:00:00Z
      String pubDate = "Fri, 29 May 2026 00:00:00 +0900";

      // when
      Optional<Instant> result = NaverNewsClient.parseNaverDate(pubDate);

      // then
      assertThat(result).contains(Instant.parse("2026-05-28T15:00:00Z"));
    }

    @Test
    @DisplayName("파싱 불가능한 날짜는 Optional.empty를 반환한다")
    void 파싱_불가능한_날짜는_Optional_empty를_반환한다() {
      // when
      Optional<Instant> result = NaverNewsClient.parseNaverDate("invalid-date");

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 날짜는 Optional.empty를 반환한다")
    void null_날짜는_Optional_empty를_반환한다() {
      // when
      Optional<Instant> result = NaverNewsClient.parseNaverDate(null);

      // then
      assertThat(result).isEmpty();
    }
  }
}
