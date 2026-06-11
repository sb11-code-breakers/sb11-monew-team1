package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriptionCreatedEvent(
    UUID userId,
    UUID subscriptionId,
    UUID interestId,
    String interestName,
    List<String> interestKeywords,
    long interestSubscriberCount,
    Instant createdAt
) {}
