package com.sprint.mission.monew.domain.article.dto;

import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArticleQueryCondition(
    String keyword,
    UUID interestId,
    List<ArticleSource> sourceIn,
    Instant publishDateFrom,
    Instant publishDateTo,
    @NotNull ArticleOrderBy orderBy,
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

  @AssertTrue(message = "cursor 형식이 orderBy 기준과 맞지 않습니다")
  public boolean isCursorFormatValidForOrderBy() {
    if (cursor == null || orderBy == null) {
      return true;
    }
    return switch (orderBy) {
      case PUBLISH_DATE -> {
        try {
          Instant.parse(cursor);
          yield true;
        } catch (Exception e) {
          yield false;
        }
      }
      case COMMENT_COUNT, VIEW_COUNT -> {
        try {
          Integer.parseInt(cursor);
          yield true;
        } catch (NumberFormatException e) {
          yield false;
        }
      }
    };
  }
}
