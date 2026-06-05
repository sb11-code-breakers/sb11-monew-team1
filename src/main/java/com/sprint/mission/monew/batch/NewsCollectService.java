package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.service.InterestNotificationService;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NewsCollectService {

  private final ArticleUpsertService articleUpsertService;
  private final NaverNewsClient naverNewsClient;
  private final RssNewsParser rssNewsParser;
  private final NewsCollectMetrics newsCollectMetrics;
  private final InterestNotificationService interestNotificationService;

  // 네트워크 호출이 포함되므로 트랜잭션 없이 실행, upsertAll은 ArticleUpsertService의 @Transactional로 처리
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void collect() {
    Instant batchStartTime = Instant.now();
    long start = System.nanoTime();
    try {
      collectNaver();
      collectRss(ArticleSource.HANKYUNG);
      collectRss(ArticleSource.CHOSUN);
      collectRss(ArticleSource.YONHAP);
    } finally {
      newsCollectMetrics.recordCollectDuration(Duration.ofNanos(System.nanoTime() - start));
    }
    interestNotificationService.notifyNewArticles(batchStartTime);
  }

  private void collectNaver() {
    try {
      List<NaverNewsItem> items = naverNewsClient.fetchNews();
      List<ArticleCandidate> candidates = new ArrayList<>();
      for (NaverNewsItem item : items) {
        Optional<Instant> publishDate = NaverNewsClient.parseNaverDate(item.pubDate());
        if (publishDate.isEmpty()) {
          log.warn("날짜 파싱 실패로 기사를 건너뜁니다: link={}", item.link());
          continue;
        }
        String sourceUrl = item.originallink() != null && !item.originallink().isBlank()
            ? item.originallink() : item.link();
        if (sourceUrl == null || sourceUrl.isBlank()) {
          log.warn("sourceUrl이 없어 기사를 건너뜁니다: pubDate={}", item.pubDate());
          continue;
        }
        String title = NaverNewsClient.stripHtml(item.title());
        String summary = NaverNewsClient.stripHtml(item.description());
        candidates.add(new ArticleCandidate(sourceUrl, title, publishDate.get(), summary));
      }
      articleUpsertService.upsertAll(ArticleSource.NAVER, candidates);
      newsCollectMetrics.countCollected(ArticleSource.NAVER, candidates.size());
      log.info("Naver 뉴스 수집 완료 | count={}", candidates.size());
    } catch (Exception e) {
      log.error("Naver 뉴스 수집 실패", e);
    }
  }

  private void collectRss(ArticleSource source) {
    try {
      List<RssArticleDto> items = rssNewsParser.parse(source);
      List<ArticleCandidate> candidates = items.stream()
          .filter(item -> item.sourceUrl() != null && !item.sourceUrl().isBlank())
          .map(item -> new ArticleCandidate(
              item.sourceUrl(), item.title(), item.publishDate(), item.summary()))
          .toList();
      if (candidates.size() != items.size()) {
        log.warn("{} RSS 기사 중 sourceUrl 누락 항목 {}건을 건너뜁니다",
            source, items.size() - candidates.size());
      }
      articleUpsertService.upsertAll(source, candidates);
      newsCollectMetrics.countCollected(source, candidates.size());
      log.info("{} RSS 수집 완료 | count={}", source, candidates.size());
    } catch (Exception e) {
      log.error("{} RSS 수집 실패", source, e);
    }
  }
}
