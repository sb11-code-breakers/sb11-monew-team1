package com.sprint.mission.monew.domain.notification.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 알림 발행 커스텀 메트릭을 집계한다. (트리거 유형별 발행 건수)
 */
@Component
public class NotificationMetrics {

  private static final String PUBLISHED = "monew.notification.published";
  private static final String TYPE_ARTICLE = "ARTICLE";
  private static final String TYPE_COMMENT_LIKE = "COMMENT_LIKE";

  private final MeterRegistry registry;

  public NotificationMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void countArticleNotifications(int count) {
    publishedCounter(TYPE_ARTICLE).increment(count);
  }

  public void countCommentLikeNotification() {
    publishedCounter(TYPE_COMMENT_LIKE).increment();
  }

  private Counter publishedCounter(String type) {
    return registry.counter(PUBLISHED, "type", type);
  }
}
