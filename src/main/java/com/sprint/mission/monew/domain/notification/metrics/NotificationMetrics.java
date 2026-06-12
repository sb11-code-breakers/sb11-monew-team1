package com.sprint.mission.monew.domain.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 알림 커스텀 메트릭을 집계한다. (만료 정리 삭제 건수 / 발행 실패 건수)
 */
@Component
public class NotificationMetrics {

  private static final String DELETED = "monew.notification.deleted";
  private static final String FAILED = "monew.notification.failed";
  private static final String TYPE_COMMENT_LIKE = "COMMENT_LIKE";
  private static final String JOB_DURATION = "monew.notification.cleanup.job.duration";
  private static final String STEP_DURATION = "monew.notification.cleanup.step.duration";
  private static final String LAST_SUCCESS = "monew.notification.cleanup.last_success.timestamp";

  private final MeterRegistry registry;
  private final Counter deletedCounter;
  private final Timer jobDurationTimer;
  private final Timer stepDurationTimer;
  private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0);

  public NotificationMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.jobDurationTimer = Timer.builder(JOB_DURATION)
        .description("알림 삭제 Job 전체 실행 시간")
        .register(registry);
    this.stepDurationTimer = Timer.builder(STEP_DURATION)
        .description("알림 삭제 Step 처리 시간")
        .register(registry);
    this.deletedCounter = Counter.builder(DELETED)
        .description("만료되어 자동 정리된 알림 수")
        .register(registry);
    Gauge.builder(LAST_SUCCESS, lastSuccessEpochSeconds, AtomicLong::get)
        .baseUnit("seconds")
        .description("알림 물리 삭제 배치가 마지막으로 정상 완료된 시각(epoch seconds)")
        .register(registry);
  }

  public void markSuccess() {
    lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
  }

  public void recordJobDuration(Duration duration) {
    jobDurationTimer.record(duration);
  }

  public void recordStepDuration(Duration duration) {
    stepDurationTimer.record(duration);
  }

  public void countDeleted(long count) {
    deletedCounter.increment(count);
  }

  public void countCommentLikeFailure() {
    registry.counter(FAILED, "type", TYPE_COMMENT_LIKE).increment();
  }
}
