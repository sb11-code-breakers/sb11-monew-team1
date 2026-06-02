package com.sprint.mission.monew.domain.user.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 사용자 커스텀 메트릭을 집계한다. (만료 사용자 물리 삭제 건수)
 */
@Component
public class UserMetrics {

  private static final String DELETED = "monew.user.deleted";

  private final Counter deletedCounter;

  public UserMetrics(MeterRegistry registry) {
    this.deletedCounter = Counter.builder(DELETED)
        .description("만료되어 물리 삭제된 사용자 수")
        .register(registry);
  }

  public void countDeleted(int count) {
    deletedCounter.increment(count);
  }
}
