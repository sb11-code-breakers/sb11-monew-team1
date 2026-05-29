package com.sprint.mission.monew.domain.article.dto;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleViewResponse(
    UUID id,
    UUID viewedBy,
    Instant createdAt,
    UUID articleId,
    ArticleSource source,
    String sourceUrl,
    String articleTitle,
    Instant articlePublishedDate,
    String articleSummary,
    int articleCommentCount,
    int articleViewCount
) {}
