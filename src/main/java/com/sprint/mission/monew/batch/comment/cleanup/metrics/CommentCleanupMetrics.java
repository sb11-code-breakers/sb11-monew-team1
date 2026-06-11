package com.sprint.mission.monew.batch.comment.cleanup.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CommentCleanupMetrics {

  private static final String DELETED = "monew.comment.deleted";

  private final Counter deletedCounter;

  public CommentCleanupMetrics(MeterRegistry registry) {
    this.deletedCounter = Counter.builder(DELETED)
        .description("만료되어 물리 삭제된 댓글 수")
        .register(registry);
  }

  public void countDeleted(long count) {
    deletedCounter.increment(count);
  }
}
