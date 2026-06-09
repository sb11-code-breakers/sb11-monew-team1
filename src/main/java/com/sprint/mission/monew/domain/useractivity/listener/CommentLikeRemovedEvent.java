package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record CommentLikeRemovedEvent(UUID userId, UUID commentId) {
}