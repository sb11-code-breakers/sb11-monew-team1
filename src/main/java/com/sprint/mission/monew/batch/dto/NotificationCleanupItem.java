package com.sprint.mission.monew.batch.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationCleanupItem(
    UUID id,
    Instant confirmedAt
) {

}
