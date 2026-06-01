package com.sprint.mission.monew.domain.useractivity.activityresponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserActivityResponse(
    UUID id,
    String email,
    String nickname,
    Instant createdAt,
    List<SubscriptionActivityResponse> subscriptions,
    List<CommentActivityResponse> comments,
    List<CommentLikeActivityResponse> commentLikes,
    List<ArticleViewActivityResponse> articleViews
) {

}