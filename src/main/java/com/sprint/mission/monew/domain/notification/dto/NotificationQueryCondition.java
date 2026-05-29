package com.sprint.mission.monew.domain.notification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;

public record NotificationQueryCondition(
    UUID cursor,

    Instant after,

    @Min(value = 1, message = "limit must be at least 1")
    @Max(value = 100, message = "limit must not exceed 100")
    Integer limit
) {

  private static final int DEFAULT_LIMIT = 50;

  public NotificationQueryCondition {
    if (limit == null) {
      limit = DEFAULT_LIMIT;
    }
  }

  @AssertTrue(message = "cursor와 after는 함께 입력해야 합니다")
  public boolean isCursorPaired() {
    return (cursor == null) == (after == null);
  }
}
