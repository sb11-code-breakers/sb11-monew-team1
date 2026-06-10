package com.sprint.mission.monew.domain.useractivity.listener;

import java.time.Instant;
import java.util.UUID;

public record CommentLikedEvent(
    UUID userId,          // 좋아요를 누른 유저
    UUID likeId,
    Instant createdAt,    // 좋아요를 누른 시점
    UUID commentId,       // 대상 댓글 ID
    UUID articleId,       // 대상 게시글 ID
    String articleTitle,   // 게시글 제목
    Instant commentCreatedAt
) {}