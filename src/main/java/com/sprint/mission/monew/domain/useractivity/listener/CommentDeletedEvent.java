package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record CommentDeletedEvent(UUID authorId, UUID commentId) {}
