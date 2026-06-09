package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record CommentLikedEvent(UUID userId, UUID commentId) {
}