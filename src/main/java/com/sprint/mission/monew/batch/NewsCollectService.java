package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Duration;
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

  // 네트워크 호출이 포함되므로 트랜잭션 없이 실행, upsert는 ArticleUpsertService의 @Transactional로 처리
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void collect() {
    long start = System.nanoTime();
    try {
      collectNaver();
      collectRss(ArticleSource.HANKYUNG);
      collectRss(ArticleSource.CHOSUN);
      collectRss(ArticleSource.YONHAP);
    } finally {
      newsCollectMetrics.recordCollectDuration(Duration.ofNanos(System.nanoTime() - start));
    }
  }

  private void collectNaver() {
    try {
      List<NaverNewsItem> items = naverNewsClient.fetchNews();
      int nUpserted = 0;
      for (NaverNewsItem item : items) {
        try {
          Optional<java.time.Instant> publishDate = NaverNewsClient.parseNaverDate(item.pubDate());
          if (publishDate.isEmpty()) {
            log.warn("날짜 파싱 실패로 기사를 건너뜁니다: link={}", item.link());
            continue;
          }
          String sourceUrl = item.originallink() != null && !item.originallink().isBlank()
              ? item.originallink() : item.link();
          String title = NaverNewsClient.stripHtml(item.title());
          String summary = NaverNewsClient.stripHtml(item.description());
          articleUpsertService.upsert(ArticleSource.NAVER, sourceUrl, title,
              publishDate.get(), summary);
          nUpserted++;
        } catch (Exception e) {
          newsCollectMetrics.countFailed(ArticleSource.NAVER);
          log.warn("Naver 기사 단건 처리 실패: link={}", item.link(), e);
        }
      }
      newsCollectMetrics.countCollected(ArticleSource.NAVER, nUpserted);
      log.info("Naver 뉴스 수집 완료: {}건", nUpserted);
    } catch (Exception e) {
      log.error("Naver 뉴스 수집 실패", e);
    }
  }

  private void collectRss(ArticleSource source) {
    try {
      List<RssArticleDto> items = rssNewsParser.parse(source);
      for (RssArticleDto item : items) {
        try {
          articleUpsertService.upsert(
              source, item.sourceUrl(), item.title(), item.publishDate(), item.summary());
        } catch (Exception e) {
          newsCollectMetrics.countFailed(source);
          log.warn("{} 기사 단건 처리 실패: url={}", source, item.sourceUrl(), e);
        }
      }
      newsCollectMetrics.countCollected(source, items.size());
      log.info("{} RSS 수집 완료: {}건", source, items.size());
    } catch (Exception e) {
      log.error("{} RSS 수집 실패", source, e);
    }
  }
}
