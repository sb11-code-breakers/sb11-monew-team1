package com.sprint.mission.monew.domain.interest.dto;

import com.sprint.mission.monew.common.dto.SortDirection;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record InterestQueryCondition(
    String keyword,
    @NotNull InterestOrderBy orderBy,
    @NotNull SortDirection direction,
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

  @AssertTrue(message = "subscriberCount 기준 커서는 숫자여야 합니다")
  public boolean isCursorFormatValidForOrderBy() {
    if (cursor == null || orderBy != InterestOrderBy.SUBSCRIBER_COUNT) {
      return true;
    }
    try {
      Long.parseLong(cursor);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}