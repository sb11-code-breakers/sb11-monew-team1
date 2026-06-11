package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.useractivity.activityresponse.ArticleViewActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.CommentActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.CommentLikeActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.SubscriptionActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserActivityService {

  private final UserActivityMongoRepository userActivityMongoRepository;

  public UserActivityResponse getUserActivity(UUID userId, UUID requestUserId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }

    UserActivity activity = userActivityMongoRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    List<SubscriptionActivityResponse> subscriptions = activity.getSubscriptions().stream()
        .map(this::toSubscriptionResponse).toList();

    List<CommentActivityResponse> comments = activity.getComments().stream()
        .map(this::toCommentResponse).toList();

    List<CommentLikeActivityResponse> commentLikes = activity.getCommentLikes().stream()
        .map(this::toCommentLikeResponse).toList();

    List<ArticleViewActivityResponse> articleViews = activity.getArticleViews().stream()
        .map(this::toArticleViewResponse).toList();

    log.info("활동 내역 조회 완료: userId={}", userId);

    return new UserActivityResponse(
        activity.getId(), activity.getEmail(), activity.getNickname(), activity.getCreatedAt(),
        subscriptions, comments, commentLikes, articleViews
    );
  }

  private SubscriptionActivityResponse toSubscriptionResponse(RecentSubscription s) {
    return new SubscriptionActivityResponse(
        s.getSubscriptionId(), s.getInterestId(), s.getInterestName(),
        s.getInterestKeywords(), s.getInterestSubscriberCount(), s.getSubscribedAt()
    );
  }

  private CommentActivityResponse toCommentResponse(RecentComment c) {
    return new CommentActivityResponse(
        c.getCommentId(), c.getArticleId(), c.getArticleTitle(),
        c.getUserId(), c.getUserNickname(), c.getContent(), c.getLikeCount(), c.getCreatedAt()
    );
  }

  private CommentLikeActivityResponse toCommentLikeResponse(RecentCommentLike cl) {
    return new CommentLikeActivityResponse(
        cl.getLikeId(), cl.getLikedAt(),
        cl.getCommentId(), cl.getArticleId(), cl.getArticleTitle(),
        cl.getCommentUserId(), cl.getCommentUserNickname(), cl.getCommentContent(),
        cl.getCommentLikeCount(), cl.getCommentCreatedAt()
    );
  }

  private ArticleViewActivityResponse toArticleViewResponse(RecentArticleView av) {
    return new ArticleViewActivityResponse(
        av.getArticleViewId(), av.getViewedBy(), av.getViewedAt(),
        av.getArticleId(), av.getSource(), av.getSourceUrl(), av.getArticleTitle(),
        av.getArticlePublishedDate(), av.getArticleSummary(),
        av.getArticleCommentCount(), av.getArticleViewCount()
    );
  }
}
