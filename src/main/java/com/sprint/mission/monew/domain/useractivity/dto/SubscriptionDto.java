package com.sprint.mission.monew.domain.useractivity.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionDto(
    UUID id,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    long interestSubscriberCount,
    Instant createdAt
) {}