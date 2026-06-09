package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record ArticleViewedEvent(UUID userId, UUID articleId) {
}