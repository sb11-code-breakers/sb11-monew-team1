package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record CommentCreatedEvent(
    UUID userId, // 이 활동을 기록할 유저 (주로 작성자 본인)
    UUID commentId,
    UUID articleId,
    String articleTitle,
    String userNickname,
    String content,
    long likeCount,
    Instant createdAt
) {}