package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record ArticleDeletedEvent(UUID userId, UUID articleId) {
}