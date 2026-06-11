package com.sprint.mission.monew.batch.news.collect.reader;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.batch.news.collect.dto.NewsCollectItem;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.external.naver.NaverNewsClient;
import com.sprint.mission.monew.external.naver.dto.NaverNewsItem;
import com.sprint.mission.monew.external.rss.RssNewsParser;
import com.sprint.mission.monew.external.rss.dto.RssArticleDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class NewsCollectReader implements ItemReader<NewsCollectItem> {

  private final NaverNewsClient naverNewsClient;
  private final RssNewsParser rssNewsParser;
  private final NewsCollectMetrics newsCollectMetrics;

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
                  collectRss(ArticleSource.YONHAP))
          )
      ).iterator();
    }

    return iterator.hasNext() ? iterator.next() : null;
  }

  private Stream<NewsCollectItem> collectNaver() {

    try {
      List<NaverNewsItem> naverItems = naverNewsClient.fetchNews();

      int successCount = 0;

      List<NewsCollectItem> result = new ArrayList<>();

      for (NaverNewsItem item : naverItems) {

        Optional<Instant> publishDate =
            NaverNewsClient.parseNaverDate(item.pubDate());

        if (publishDate.isEmpty()) {
          log.warn("날짜 파싱 실패로 기사를 건너뜁니다: link={}", item.link());
          continue;
        }

        String sourceUrl =
            item.originallink() != null && !item.originallink().isBlank()
                ? item.originallink()
                : item.link();

        if (sourceUrl == null || sourceUrl.isBlank()) {
          log.warn("sourceUrl이 없어 기사를 건너뜁니다: pubDate={}", item.pubDate());
          continue;
        }

        result.add(new NewsCollectItem(
            ArticleSource.NAVER,
            sourceUrl,
            NaverNewsClient.stripHtml(item.title()),
            publishDate.get(),
            NaverNewsClient.stripHtml(item.description())
        ));

        successCount++;
      }

      newsCollectMetrics.countCollected(ArticleSource.NAVER, successCount);
      log.info("Naver 뉴스 수집 완료 | count={}", successCount);

      return result.stream();

    } catch (Exception e) {
      log.error("Naver 뉴스 수집 실패", e);
      newsCollectMetrics.countFailed(ArticleSource.NAVER);

      return Stream.empty();
    }
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
        log.warn("{} RSS 기사 중 sourceUrl 누락 {}건을 건너뜁니다",
            source, skipped);
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