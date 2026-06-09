package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record SubscriptionCancelledEvent(UUID userId, UUID interestId) {
}