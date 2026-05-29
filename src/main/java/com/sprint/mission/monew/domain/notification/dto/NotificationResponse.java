package com.sprint.mission.monew.domain.notification.dto;

import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    boolean confirmed,
    UUID userId,
    String content,
    ResourceType resourceType,
    UUID resourceId
) {

}
