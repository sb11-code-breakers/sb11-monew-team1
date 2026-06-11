package com.sprint.mission.monew.domain.useractivity.listener;

import java.util.UUID;

public record CommentUpdatedEvent(UUID commentId, String content) {}
