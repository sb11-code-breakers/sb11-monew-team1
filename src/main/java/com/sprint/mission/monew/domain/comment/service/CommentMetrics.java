package com.sprint.mission.monew.domain.comment.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 댓글·좋아요 커스텀 메트릭을 집계한다. (댓글 작성 수 / 좋아요 등록·취소 수)
 */
@Component
public class CommentMetrics {

  private static final String CREATED = "monew.comment.created";
  private static final String LIKE = "monew.comment.like";
  private static final String ACTION_CREATE = "create";
  private static final String ACTION_CANCEL = "cancel";

  private final MeterRegistry registry;
  private final Counter createdCounter;

  public CommentMetrics(MeterRegistry registry) {
    this.registry = registry;
    this.createdCounter = Counter.builder(CREATED)
        .description("작성된 댓글 수")
        .register(registry);
  }

  public void countCreated() {
    createdCounter.increment();
  }

  public void countLiked() {
    registry.counter(LIKE, "action", ACTION_CREATE).increment();
  }

  public void countLikeCanceled() {
    registry.counter(LIKE, "action", ACTION_CANCEL).increment();
  }
}
