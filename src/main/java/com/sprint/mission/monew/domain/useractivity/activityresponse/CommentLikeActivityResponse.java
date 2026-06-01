package com.sprint.mission.monew.domain.useractivity.activityresponse;

import java.time.Instant;
import java.util.UUID;

public record CommentLikeActivityResponse(
    UUID id,
    Instant createdAt,
    UUID commentId,
    UUID articleId,
    String articleTitle,
    UUID commentUserId,
    String commentUserNickname,
    String commentContent,
    long commentLikeCount,
    Instant commentCreatedAt
) {

}