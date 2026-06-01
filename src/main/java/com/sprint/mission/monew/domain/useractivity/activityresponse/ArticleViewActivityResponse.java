package com.sprint.mission.monew.domain.useractivity.activityresponse;

import java.time.Instant;
import java.util.UUID;

public record ArticleViewActivityResponse(
    UUID id,
    UUID viewedBy,
    Instant createdAt,
    UUID articleId,
    String source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    long articleCommentCount,
    long articleViewCount
) {

}