package com.sprint.mission.monew.domain.comment.dto;

import com.sprint.mission.monew.common.dto.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CommentQueryCondition(
    @Schema(description = "기사 ID") UUID articleId,
    @Schema(description = "정렬 속성 이름") @NotNull CommentOrderBy orderBy,
    @Schema(description = "정렬 방향") @NotNull SortDirection direction,
    @Schema(description = "커서 값") String cursor,
    @Schema(description = "보조 커서 값") Instant after,
    @Schema(description = "커서 페이지 크기", example = "50") @NotNull @Min(1) Integer limit
) {

  @AssertTrue(message = "cursor와 after는 함께 전달되어야 합니다.")
  public boolean isCursorAndAfterConsistent() {
    return (cursor == null) == (after == null);
  }

  @AssertTrue(message = "likeCount cursor는 숫자여야 합니다.")
  public boolean isLikeCountCursorFormatValidOrderBy() {
    if (cursor == null || orderBy != CommentOrderBy.LIKE_COUNT) {
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
