package com.sprint.mission.monew.batch.notification.cleanup.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationCleanupItem(
    UUID id,
    Instant confirmedAt
) {

}
