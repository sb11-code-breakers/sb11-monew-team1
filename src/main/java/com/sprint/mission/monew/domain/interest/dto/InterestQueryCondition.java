package com.sprint.mission.monew.domain.interest.dto;

import com.sprint.mission.monew.common.dto.SortDirection;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record InterestQueryCondition(
    String keyword,
    @NotNull InterestOrderBy orderBy,
    @NotNull SortDirection direction,
    String cursor,
    Instant after,
    @NotNull @Min(1) Integer limit
) {

  @AssertTrue(message = "cursor와 after는 함께 전달되어야 합니다")
  public boolean isCursorAndAfterConsistent() {
    return (cursor == null) == (after == null);
  }

  @AssertTrue(message = "subscriberCount 기준 커서는 숫자여야 합니다")
  public boolean isCursorFormatValidForOrderBy() {
    if (cursor == null || orderBy != InterestOrderBy.SUBSCRIBER_COUNT) {
      return true;
    }
    try {
      Long.valueOf(cursor);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}