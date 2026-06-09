package com.sprint.mission.monew.domain.article.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleBackupEntry(
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary) {}
