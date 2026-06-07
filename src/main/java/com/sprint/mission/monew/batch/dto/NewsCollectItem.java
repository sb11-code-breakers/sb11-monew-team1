package com.sprint.mission.monew.batch.dto;

import com.sprint.mission.monew.domain.article.entity.ArticleSource;
import java.time.Instant;

public record NewsCollectItem(
    ArticleSource source,
    String sourceUrl,
    String title,
    Instant publishDate,
    String summary
) {

}
