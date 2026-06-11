package com.sprint.mission.monew.batch.news.collect.writer;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.batch.news.collect.dto.ArticleCandidate;
import com.sprint.mission.monew.batch.news.collect.dto.NewsCollectItem;
import com.sprint.mission.monew.batch.news.collect.service.ArticleUpsertService;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import com.sprint.mission.monew.domain.interest.service.InterestNotificationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectWriter implements ItemWriter<NewsCollectItem> {

  private final ArticleUpsertService articleUpsertService;
  private final NewsCollectMetrics newsCollectMetrics;
  private final InterestNotificationService interestNotificationService;

  @Override
  public void write(Chunk<? extends NewsCollectItem> chunk) {

    Instant batchStartTime = Instant.now();

    Map<ArticleSource, List<ArticleCandidate>> grouped =
        new EnumMap<>(ArticleSource.class);

    for (NewsCollectItem item : chunk.getItems()) {

      grouped.computeIfAbsent(item.source(), k -> new ArrayList<>())
          .add(new ArticleCandidate(
              item.sourceUrl(),
              item.title(),
              item.publishDate(),
              item.summary()
          ));
    }

    grouped.forEach((source, candidates) -> {
      try {

        if (candidates.isEmpty()) {
          return;
        }

        articleUpsertService.upsertAll(source, candidates);
        newsCollectMetrics.countCollected(source, candidates.size());

        log.info("{} 뉴스 수집 완료 | count={}", source, candidates.size());

      } catch (Exception e) {
        newsCollectMetrics.countFailed(source);
        log.error("{} 뉴스 수집 실패", source, e);
      }
    });

    try {
      interestNotificationService.notifyNewArticles(batchStartTime);
    } catch (Exception e) {
      log.error("알림 전송 실패", e);
    }
  }
}