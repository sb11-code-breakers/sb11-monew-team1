package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
    UUID userId,
    UUID likeId,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    long commentLikeCount,
    Instant commentCreatedAt
) {}
