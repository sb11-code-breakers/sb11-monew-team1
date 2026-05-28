package com.sprint.mission.monew.domain.article.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ArticleOrderBy {
  @JsonProperty("publishDate") PUBLISH_DATE,
  @JsonProperty("viewCount") VIEW_COUNT,
  @JsonProperty("commentCount") COMMENT_COUNT
}
