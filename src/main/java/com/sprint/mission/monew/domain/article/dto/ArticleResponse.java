package com.sprint.mission.monew.domain.article.dto;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(
    UUID id,
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary,
    int commentCount,
    int viewCount,
    boolean viewedByMe) {}
