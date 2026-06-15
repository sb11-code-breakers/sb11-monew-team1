package com.sprint.mission.monew.batch.news.collect.reader;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.batch.news.collect.dto.NewsCollectItem;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class NewsCollectReader implements ItemReader<NewsCollectItem> {

  private final NaverNewsClient naverNewsClient;
  private final RssNewsParser rssNewsParser;
  private final InterestRepository interestRepository;
  private final NewsCollectMetrics newsCollectMetrics;

  @Value("${monew.naver.daily-limit}")
  private int naverDailyLimit;

  @Value("${monew.naver.batch-frequency-per-day}")
  private int batchFrequencyPerDay;

  @Value("${monew.naver.max-pages}")
  private int naverMaxPages;

  @Value("${monew.naver.lookback-hours}")
  private long lookbackHours;

  private Iterator<NewsCollectItem> iterator;

  @Override
  public NewsCollectItem read() {
    if (iterator == null) {
      iterator = Stream.concat(
          collectNaver(),
          Stream.concat(
              collectRss(ArticleSource.HANKYUNG),
              Stream.concat(
                  collectRss(ArticleSource.CHOSUN),
                  collectRss(ArticleSource.YONHAP)))
      ).iterator();
    }
    return iterator.hasNext() ? iterator.next() : null;
  }

  private Stream<NewsCollectItem> collectNaver() {
    List<String> keywords = extractKeywords();
    if (keywords.isEmpty()) {
      log.info("등록된 관심사 키워드가 없어 Naver 수집을 건너뜁니다");
      return Stream.empty();
    }

    int quota = batchFrequencyPerDay > 0
        ? naverDailyLimit / batchFrequencyPerDay / keywords.size()
        : naverDailyLimit / keywords.size();
    int maxPages = Math.max(Math.min(quota, naverMaxPages), 1);
    Instant cutoff = Instant.now().minus(lookbackHours, ChronoUnit.HOURS);

    log.info("Naver 수집 시작 | keywordCount={}, maxPagesPerKeyword={}", keywords.size(), maxPages);

    List<NewsCollectItem> result = new ArrayList<>();
    for (String keyword : keywords) {
      result.addAll(collectNaverByKeyword(keyword, maxPages, cutoff));
    }

    newsCollectMetrics.countCollected(ArticleSource.NAVER, result.size());
    log.info("Naver 수집 완료 | totalCount={}", result.size());
    return result.stream();
  }

  private List<String> extractKeywords() {
    return interestRepository.findAllWithKeywords().stream()
        .flatMap(interest -> interest.getKeywords().stream())
        .map(k -> k.getKeyword())
        .distinct()
        .toList();
  }

  private List<NewsCollectItem> collectNaverByKeyword(String keyword, int maxPages, Instant cutoff) {
    List<NewsCollectItem> result = new ArrayList<>();
    for (int page = 1; page <= maxPages; page++) {
      try {
        List<NaverNewsItem> items = naverNewsClient.fetchNews(keyword, page);
        if (items.isEmpty()) {
          break;
        }

        for (NaverNewsItem item : items) {
          parseNaverItem(item).ifPresent(result::add);
        }

        NaverNewsItem last = items.get(items.size() - 1);
        Optional<Instant> lastPubDate = NaverNewsClient.parseNaverDate(last.pubDate());
        if (lastPubDate.isEmpty() || lastPubDate.get().isBefore(cutoff)) {
          break;
        }
      } catch (Exception e) {
        log.error("Naver 뉴스 수집 실패 | keyword={}, page={}", keyword, page, e);
        newsCollectMetrics.countFailed(ArticleSource.NAVER);
        break;
      }
    }
    log.info("Naver 키워드 수집 완료 | keyword={}, count={}", keyword, result.size());
    return result;
  }

  private Optional<NewsCollectItem> parseNaverItem(NaverNewsItem item) {
    Optional<Instant> publishDate = NaverNewsClient.parseNaverDate(item.pubDate());
    if (publishDate.isEmpty()) {
      log.warn("날짜 파싱 실패로 기사를 건너뜁니다: link={}", item.link());
      return Optional.empty();
    }

    String sourceUrl = item.originallink() != null && !item.originallink().isBlank()
        ? item.originallink()
        : item.link();
    if (sourceUrl == null || sourceUrl.isBlank()) {
      log.warn("sourceUrl이 없어 기사를 건너뜁니다: pubDate={}", item.pubDate());
      return Optional.empty();
    }

    return Optional.of(new NewsCollectItem(
        ArticleSource.NAVER,
        sourceUrl,
        NaverNewsClient.stripHtml(item.title()),
        publishDate.get(),
        NaverNewsClient.stripHtml(item.description())
    ));
  }

  private Stream<NewsCollectItem> collectRss(ArticleSource source) {
    try {
      List<RssArticleDto> rssItems = rssNewsParser.parse(source);

      int successCount = 0;
      int skipped = 0;
      List<NewsCollectItem> result = new ArrayList<>();

      for (RssArticleDto item : rssItems) {
        if (item.sourceUrl() == null || item.sourceUrl().isBlank()) {
          skipped++;
          continue;
        }
        result.add(new NewsCollectItem(
            source,
            item.sourceUrl(),
            item.title(),
            item.publishDate(),
            item.summary()
        ));
        successCount++;
      }

      if (skipped > 0) {
        log.warn("{} RSS 기사 중 sourceUrl 누락 {}건을 건너뜁니다", source, skipped);
      }

      newsCollectMetrics.countCollected(source, successCount);
      log.info("{} RSS 수집 완료 | count={}", source, successCount);
      return result.stream();

    } catch (Exception e) {
      log.error("{} RSS 수집 실패", source, e);
      newsCollectMetrics.countFailed(source);
      return Stream.empty();
    }
  }
}
