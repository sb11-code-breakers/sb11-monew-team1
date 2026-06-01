package com.sprint.mission.monew.domain.useractivity.activityresponse;

import java.time.Instant;
import java.util.UUID;

public record CommentActivityResponse(
    UUID id,
    UUID articleId,
    String articleTitle,
    UUID userId,
    String userNickname,
    String content,
    long likeCount,
    Instant createdAt
) {

}