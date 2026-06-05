package com.sprint.mission.monew.domain.notification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record NotificationQueryCondition(
    Instant cursor,
    Instant after,
    @NotNull @Min(1) Integer limit
) {

  @AssertTrue(message = "cursor와 after는 함께 전달되어야 합니다")
  public boolean isCursorAndAfterConsistent() {
    return (cursor == null) == (after == null);
  }
}
