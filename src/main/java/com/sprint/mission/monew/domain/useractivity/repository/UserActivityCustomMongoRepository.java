package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.util.UUID;

public interface UserActivityCustomMongoRepository {
  void createUserActivity(UserActivity userActivity);
  void updateNickname(UUID userId, String nickname);
  void anonymize(UUID userId);
  void anonymizeCommentLikesByCommentUserId(UUID userId);

  void pushComment(UUID userId, RecentComment comment);
  void updateCommentContent(UUID commentId, String content);
  void pushSubscription(UUID userId, RecentSubscription subscription);
  void pushCommentLike(UUID userId, RecentCommentLike commentLike);
  void pushArticleView(UUID userId, RecentArticleView articleView);

  void pullComment(UUID userId, UUID commentId);
  void pullSubscription(UUID userId, UUID interestId);
  void pullCommentLike(UUID userId, UUID commentId);

  void pullArticleViewsByArticleId(UUID articleId);
  void pullCommentsByArticleId(UUID articleId);
  void pullCommentLikesByArticleId(UUID articleId);
  void pullSubscriptionsByInterestId(UUID interestId);
}