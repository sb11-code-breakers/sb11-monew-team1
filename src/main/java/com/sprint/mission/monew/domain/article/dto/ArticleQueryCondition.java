package com.sprint.mission.monew.domain.article.dto;

import com.sprint.mission.monew.common.dto.SortDirection;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
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
    @Min(1) int limit
) {}
