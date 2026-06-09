package com.sprint.mission.monew.domain.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 알림 커스텀 메트릭을 집계한다. (만료 정리 삭제 건수 / 발행 실패 건수)
 */
@Component
public class NotificationMetrics {

  private static final String DELETED = "monew.notification.deleted";
  private static final String FAILED = "monew.notification.failed";
  private static final String TYPE_COMMENT_LIKE = "COMMENT_LIKE";

  private final MeterRegistry registry;
  private final Counter deletedCounter;

  public NotificationMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.deletedCounter = Counter.builder(DELETED)
        .description("만료되어 자동 정리된 알림 수")
        .register(registry);
  }

  public void countDeleted(long count) {
    deletedCounter.increment(count);
  }

  public void countCommentLikeFailure() {
    registry.counter(FAILED, "type", TYPE_COMMENT_LIKE).increment();
  }
}
