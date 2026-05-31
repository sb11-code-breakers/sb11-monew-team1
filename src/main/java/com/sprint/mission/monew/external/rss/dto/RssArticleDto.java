package com.sprint.mission.monew.external.rss.dto;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;

public record RssArticleDto(
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary
) {}
