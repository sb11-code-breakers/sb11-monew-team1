package com.sprint.mission.monew.domain.notification.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public record NotificationQueryCondition(
    String cursor,
    Instant after,
    UUID idAfter,
    @NotNull @Min(1) Integer limit
) {

  @AssertTrue(message = "cursor, after, idAfter는 함께 전달되어야 합니다")
  public boolean isCursorAndAfterAndIdAfterConsistent() {
    return (cursor == null && after == null && idAfter == null)
        || (cursor != null && after != null && idAfter != null);
  }

  @AssertTrue(message = "cursor는 createdAt(ISO-8601 Instant) 형식이어야 합니다")
  public boolean isCursorFormatValid() {
    if (cursor == null) {
      return true;
    }
    try {
      Instant.parse(cursor);
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
