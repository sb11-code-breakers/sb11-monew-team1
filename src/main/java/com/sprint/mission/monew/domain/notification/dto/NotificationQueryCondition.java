package com.sprint.mission.monew.domain.notification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record NotificationQueryCondition(
    Instant cursor,
    Instant after,
    UUID idAfter,
    @NotNull @Min(1) Integer limit
) {

  @AssertTrue(message = "cursor, after, idAfter는 함께 전달되어야 합니다")
  public boolean isCursorAndAfterAndIdAfterConsistent() {
    return (cursor == null && after == null && idAfter == null)
        || (cursor != null && after != null && idAfter != null);
  }
}
