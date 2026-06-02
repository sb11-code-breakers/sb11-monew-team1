package com.sprint.mission.monew.domain.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeResponse(
    UUID id,
    UUID likedBy,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    long commentLikeCount,
    Instant commentCreatedAt
) {

}
