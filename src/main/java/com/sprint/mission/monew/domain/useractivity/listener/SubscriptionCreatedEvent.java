package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record SubscriptionCreatedEvent(UUID userId, UUID targetId) {
}