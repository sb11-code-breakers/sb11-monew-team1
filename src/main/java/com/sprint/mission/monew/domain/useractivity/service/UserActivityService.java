package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.interest.entity.Interest;
import com.sprint.mission.monew.domain.interest.entity.InterestKeyword;
import com.sprint.mission.monew.domain.interest.repository.InterestRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
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
import com.sprint.mission.monew.domain.useractivity.projection.ArticleViewLiveData;
import com.sprint.mission.monew.domain.useractivity.projection.CommentLikeLiveData;
import com.sprint.mission.monew.domain.useractivity.projection.CommentLiveData;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserActivityService {

  private final UserRepository userRepository;
  private final UserActivityMongoRepository userActivityMongoRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleViewRepository articleViewRepository;
  private final InterestRepository interestRepository;

  public UserActivityResponse getUserActivity(UUID userId, UUID requestUserId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }

    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    // 2단계: MongoDB 불변 메타데이터 조회
    UserActivity activity = userActivityMongoRepository.findById(userId)
        .orElseGet(() -> UserActivity.of(userId, user.getCreatedAt()));

    // ID 추출
    List<UUID> commentIds = activity.getComments().stream()
        .map(RecentComment::getCommentId).toList();
    List<UUID> commentLikeCommentIds = activity.getCommentLikes().stream()
        .map(RecentCommentLike::getCommentId).toList();
    List<UUID> interestIds = activity.getSubscriptions().stream()
        .map(RecentSubscription::getInterestId).toList();
    List<UUID> articleIds = activity.getArticleViews().stream()
        .map(RecentArticleView::getArticleId).toList();

    // 3단계: PostgreSQL 최소 프로젝션
    Map<UUID, CommentLiveData> commentMap = commentIds.isEmpty() ? Map.of() :
        commentRepository.findCommentLiveDataByIds(commentIds).stream()
            .collect(Collectors.toMap(CommentLiveData::getId, Function.identity()));

    Map<UUID, CommentLikeLiveData> commentLikeMap = commentLikeCommentIds.isEmpty() ? Map.of() :
        commentLikeRepository.findCommentLikeLiveDataByUserIdAndCommentIds(userId, commentLikeCommentIds).stream()
            .collect(Collectors.toMap(CommentLikeLiveData::getCommentId, Function.identity()));

    Map<UUID, Interest> interestMap = interestIds.isEmpty() ? Map.of() :
        interestRepository.findWithKeywordsByIds(interestIds).stream()
            .collect(Collectors.toMap(Interest::getId, Function.identity()));

    Map<UUID, ArticleViewLiveData> articleViewMap = articleIds.isEmpty() ? Map.of() :
        articleViewRepository.findArticleViewLiveDataByUserIdAndArticleIds(userId, articleIds).stream()
            .collect(Collectors.toMap(ArticleViewLiveData::getArticleId, Function.identity()));

    // 4단계: Stitch
    List<SubscriptionActivityResponse> subscriptions = activity.getSubscriptions().stream()
        .map(recent -> {
          Interest interest = interestMap.get(recent.getInterestId());
          if (interest == null) return null;
          List<String> keywords = interest.getKeywords().stream()
              .map(InterestKeyword::getKeyword).toList();
          return new SubscriptionActivityResponse(
              recent.getInterestId(), recent.getInterestId(), recent.getInterestName(),
              keywords, interest.getSubscriberCount(), recent.getSubscribedAt()
          );
        })
        .filter(Objects::nonNull)
        .toList();

    List<CommentActivityResponse> comments = activity.getComments().stream()
        .map(recent -> {
          CommentLiveData live = commentMap.get(recent.getCommentId());
          if (live == null) return null;
          return new CommentActivityResponse(
              live.getId(), recent.getArticleId(), recent.getArticleTitle(),
              userId, user.getNickname(),
              live.getContent(), live.getLikeCount(), recent.getCreatedAt()
          );
        })
        .filter(Objects::nonNull)
        .toList();

    List<CommentLikeActivityResponse> commentLikes = activity.getCommentLikes().stream()
        .map(recent -> {
          CommentLikeLiveData live = commentLikeMap.get(recent.getCommentId());
          if (live == null) return null;
          return new CommentLikeActivityResponse(
              live.getId(), recent.getLikedAt(),
              recent.getCommentId(), recent.getArticleId(), recent.getArticleTitle(),
              live.getCommentUserId(), live.getCommentUserNickname(),
              live.getCommentContent(), live.getCommentLikeCount(), recent.getCommentCreatedAt()
          );
        })
        .filter(Objects::nonNull)
        .toList();

    List<ArticleViewActivityResponse> articleViews = activity.getArticleViews().stream()
        .map(recent -> {
          ArticleViewLiveData live = articleViewMap.get(recent.getArticleId());
          if (live == null) return null;
          return new ArticleViewActivityResponse(
              live.getId(), userId, recent.getViewedAt(),
              recent.getArticleId(), recent.getSource(), recent.getSourceUrl(),
              recent.getArticleTitle(), recent.getArticlePublishedDate(), recent.getArticleSummary(),
              live.getArticleCommentCount(), live.getArticleViewCount()
          );
        })
        .filter(Objects::nonNull)
        .toList();

    log.info("활동 내역 조회 완료: userId={}", userId);

    return new UserActivityResponse(
        user.getId(), user.getEmail(), user.getNickname(), user.getCreatedAt(),
        subscriptions, comments, commentLikes, articleViews
    );
  }
}
