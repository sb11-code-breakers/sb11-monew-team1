package com.sprint.mission.monew.domain.useractivity.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityResponse(
    UUID id,
    String email,
    String nickname,
    Instant createdAt,
    List<SubscriptionDto> subscriptions,
    List<CommentDto> comments,
    List<CommentLikeDto> commentLikes,
    List<ArticleViewDto> articleViews
) {

}