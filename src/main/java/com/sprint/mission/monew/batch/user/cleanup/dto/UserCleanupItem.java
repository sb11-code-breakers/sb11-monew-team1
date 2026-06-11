package com.sprint.mission.monew.batch.user.cleanup.dto;

import java.time.Instant;
import java.util.UUID;

public record UserCleanupItem(
    UUID id,
    Instant deletedAt
) {

}
