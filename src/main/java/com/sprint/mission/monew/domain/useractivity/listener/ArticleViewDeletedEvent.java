package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record ArticleViewDeletedEvent(UUID userId, UUID articleId) {}
