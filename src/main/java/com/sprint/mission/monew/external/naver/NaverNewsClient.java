package com.sprint.mission.monew.external.naver;

import com.sprint.mission.monew.common.util.HtmlUtils;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.naver.dto.NaverNewsResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsClient {

  private static final String NEWS_URL =
      "https://openapi.naver.com/v1/search/news.json?query={query}&display={display}&sort=date&start={start}";

  // "dd MMM yyyy HH:mm:ss Z" — 요일 접두어는 파싱 전에 제거해 검증 오류 방지
  private static final DateTimeFormatter NAVER_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

  private final RestClient restClient;

  @Value("${monew.naver.client-id}")
  private String clientId;

  @Value("${monew.naver.client-secret}")
  private String clientSecret;

  @Value("${monew.naver.display}")
  private int display;

  public List<NaverNewsItem> fetchNews(String keyword, int page) {
    int start = display * (page - 1) + 1;
    NaverNewsResponse response = restClient.get()
        .uri(NEWS_URL, keyword, display, start)
        .header("X-Naver-Client-Id", clientId)
        .header("X-Naver-Client-Secret", clientSecret)
        .retrieve()
        .body(NaverNewsResponse.class);

    if (response == null || response.items() == null) {
      return List.of();
    }
    return response.items();
  }

  public static Optional<Instant> parseNaverDate(String pubDate) {
    if (pubDate == null) {
      log.debug("Naver 기사 날짜가 null입니다");
      return Optional.empty();
    }
    try {
      // "Mon, 29 May 2026 ..." → "29 May 2026 ..." (요일 부분 제거)
      String dateStr = pubDate.contains(",") ? pubDate.substring(pubDate.indexOf(',') + 2) : pubDate;
      return Optional.of(OffsetDateTime.parse(dateStr, NAVER_DATE_FORMATTER).toInstant());
    } catch (Exception e) {
      log.debug("Naver 기사 날짜 파싱 실패: pubDate={}", pubDate);
      return Optional.empty();
    }
  }

  public static String stripHtml(String html) {
    return HtmlUtils.strip(html);
  }
}
