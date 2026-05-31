package com.sprint.mission.monew.domain.interest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum InterestOrderBy {
  @JsonProperty("name") NAME,
  @JsonProperty("subscriberCount") SUBSCRIBER_COUNT
}