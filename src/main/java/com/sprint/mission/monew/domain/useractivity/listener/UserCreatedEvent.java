package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedEvent(UUID userId, String email, String nickname, Instant createdAt) {
}