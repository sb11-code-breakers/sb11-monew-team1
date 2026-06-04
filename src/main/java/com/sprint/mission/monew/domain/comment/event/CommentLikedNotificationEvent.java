package com.sprint.mission.monew.domain.comment.event;

import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import java.util.UUID;

public record CommentLikedNotificationEvent(
    UUID recipientId,
    String message,
    ResourceType resourceType,
    UUID resourceId) {}
