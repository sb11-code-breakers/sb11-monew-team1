package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
    UUID userId,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    String content,
    String userNickname,
    long likeCount,
    Instant createdAt
) {}
