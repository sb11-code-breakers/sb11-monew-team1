package com.sprint.mission.monew.domain.useractivity.repository;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import java.util.UUID;

public interface UserActivityCustomMongoRepository {
  void createUserActivity(UserActivity userActivity);
  void updateNickname(UUID userId, String newNickname);
  void anonymize(UUID userId);
  void anonymizeCommentLikesByCommentUserId(UUID userId);

  // 추가관련 메소드는 객체를 다받는다
  void pushComment(UUID userId, RecentComment comment);
  void pushSubscription(UUID userId, RecentSubscription subscription);
  void pushCommentLike(UUID userId, RecentCommentLike commentLike);
  void pushArticleView(UUID userId, RecentArticleView articleView);

  //삭제는 식별자만
  void pullComment(UUID userId, UUID commentId);
  void pullArticleViewsByArticleId(UUID articleId);
  void pullCommentsByArticleId(UUID articleId);
  void pullCommentLikesByArticleId(UUID articleId);
  void pullSubscriptionsByInterestId(UUID interestId);

  void pullSubscription(UUID userId, UUID interestId);
  void updateCommentContent(UUID userId, UUID commentId, String content);
  void pullCommentLike(UUID userId, UUID commentId);
  void pullArticle(UUID userId, UUID articleId);
  void pullArticleLike(UUID userId, UUID articleId);
  void pullArticleView(UUID userId, UUID articleId);
}