package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionCreatedEvent(
    UUID userId, // 구독을 누른 유저
    UUID interestId,
    String interestName,
    Instant createdAt
) {}