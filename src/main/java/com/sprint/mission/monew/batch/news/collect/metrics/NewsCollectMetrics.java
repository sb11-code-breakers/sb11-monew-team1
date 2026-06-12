package com.sprint.mission.monew.batch.news.collect.metrics;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 과정의 커스텀 메트릭을 집계한다. (출처별 수집 건수 / 처리 결과별 건수 / 소요 시간)
 */
@Component
public class NewsCollectMetrics {

  private static final String COLLECTED = "monew.news.collected";
  private static final String PROCESSED = "monew.news.processed";
  private static final String JOB_DURATION = "monew.news.collect.job.duration";
  private static final String STEP_DURATION = "monew.news.collect.step.duration";
  private static final String LAST_SUCCESS = "monew.news.last_success.timestamp";
  private static final String SOURCE = "source";
  private static final String RESULT = "result";
  private static final String RESULT_CREATED = "created";
  private static final String RESULT_DUPLICATED = "duplicated";
  private static final String RESULT_FAILED = "failed";

  private final MeterRegistry registry;
  private final Timer jobDurationTimer;
  private final Timer stepDurationTimer;
  private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0);

  public NewsCollectMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.jobDurationTimer = Timer.builder(JOB_DURATION)
        .description("뉴스 수집 Job 전체 실행 시간")
        .register(registry);
    this.stepDurationTimer = Timer.builder(STEP_DURATION)
        .description("뉴스 수집 Step 처리 시간")
        .register(registry);
    Gauge.builder(LAST_SUCCESS, lastSuccessEpochSeconds, AtomicLong::get)
        .baseUnit("seconds")
        .description("뉴스 수집 배치가 마지막으로 정상 완료된 시각(epoch seconds)")
        .register(registry);
  }

  public void countCollected(ArticleSource source, int count) {
    registry.counter(COLLECTED, SOURCE, source.name()).increment(count);
  }

  public void countCreated(ArticleSource source) {
    processed(source, RESULT_CREATED).increment();
  }

  public void countDuplicated(ArticleSource source) {
    processed(source, RESULT_DUPLICATED).increment();
  }

  public void countFailed(ArticleSource source) {
    processed(source, RESULT_FAILED).increment();
  }

  public void recordJobDuration(Duration duration) {
    jobDurationTimer.record(duration);
  }

  public void recordStepDuration(Duration duration) {
    stepDurationTimer.record(duration);
  }

  public void markSuccess() {
    lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
  }

  private Counter processed(ArticleSource source, String result) {
    return registry.counter(PROCESSED, SOURCE, source.name(), RESULT, result);
  }
}
