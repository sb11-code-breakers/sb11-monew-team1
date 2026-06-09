package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record CommentCreatedEvent(UUID userId, UUID articleId, UUID commentId) {
}