package com.sprint.mission.monew.batch.comment.cleanup.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentCleanupItem(
    UUID id,
    Instant deletedAt
) {

}
