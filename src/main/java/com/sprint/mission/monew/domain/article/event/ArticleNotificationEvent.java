package com.sprint.mission.monew.domain.article.event;

import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import java.util.List;
import java.util.UUID;

public record ArticleNotificationEvent(
    List<UUID> recipientIds,
    String message,
    ResourceType resourceType,
    UUID resourceId) {}