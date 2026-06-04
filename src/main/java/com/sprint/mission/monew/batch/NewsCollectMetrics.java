package com.sprint.mission.monew.batch;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 과정의 커스텀 메트릭을 집계한다. (출처별 수집 건수 / 신규 / 중복 / 소요 시간)
 */
@Component
public class NewsCollectMetrics {

  private static final String COLLECTED = "monew.news.collected";
  private static final String CREATED = "monew.news.created";
  private static final String DUPLICATED = "monew.news.duplicated";
  private static final String FAILED = "monew.news.failed";
  private static final String COLLECT_DURATION = "monew.news.collect.duration";

  private final MeterRegistry registry;
  private final Counter createdCounter;
  private final Counter duplicatedCounter;
  private final Timer collectDurationTimer;

  public NewsCollectMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.createdCounter = Counter.builder(CREATED)
        .description("신규로 저장된 기사 수")
        .register(registry);
    this.duplicatedCounter = Counter.builder(DUPLICATED)
        .description("중복으로 갱신된 기사 수")
        .register(registry);
    this.collectDurationTimer = Timer.builder(COLLECT_DURATION)
        .description("뉴스 수집 1회 소요 시간")
        .register(registry);
  }

  public void countCollected(ArticleSource source, int count) {
    registry.counter(COLLECTED, "source", source.name()).increment(count);
  }

  public void countFailed(ArticleSource source) {
    registry.counter(FAILED, "source", source.name()).increment();
  }

  public void countCreated() {
    createdCounter.increment();
  }

  public void countDuplicated() {
    duplicatedCounter.increment();
  }

  public void recordCollectDuration(Duration duration) {
    collectDurationTimer.record(duration);
  }
}