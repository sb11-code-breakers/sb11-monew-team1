package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewedEvent(
    UUID userId,
    UUID articleViewId,
    Instant createdAt,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    long articleCommentCount,
    long articleViewCount
) {}
