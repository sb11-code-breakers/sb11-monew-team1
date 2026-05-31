package com.sprint.mission.monew.domain.comment.dto.response;

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
    int commentLikeCount,
    Instant commentCreatedAt
) {

}
