package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.activityresponse.ArticleViewActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.CommentActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.CommentLikeActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.SubscriptionActivityResponse;
import com.sprint.mission.monew.domain.useractivity.activityresponse.UserActivityResponse;
import com.sprint.mission.monew.domain.useractivity.mapper.UserActivityMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserActivityService {

  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeRepository commentLikeRepository;
  private final ArticleViewRepository articleViewRepository;
  private final UserActivityMapper userActivityMapper;

  public UserActivityResponse getUserActivity(UUID userId, UUID requestUserId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }

    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    List<SubscriptionActivityResponse> subscriptionActivityResponses = subscriptionRepository
        .findAllByUserId(userId, PageRequest.of(0, 10))
        .stream()
        .map(userActivityMapper::toSubscriptionDto)
        .toList();

    List<CommentActivityResponse> commentActivityResponses = commentRepository
        .findTop10RecentCommentsByUserId(userId, PageRequest.of(0, 10))
        .stream()
        .map(userActivityMapper::toCommentDto)
        .toList();

    List<CommentLikeActivityResponse> commentLikeActivityResponses = commentLikeRepository
        .findTop10ByUserId(userId, PageRequest.of(0, 10))
        .stream()
        .map(userActivityMapper::toCommentLikeDto)
        .toList();

    List<ArticleViewActivityResponse> articleViewActivityResponses = articleViewRepository
        .findTop10ByUserIdAndArticleNotDeleted(userId, PageRequest.of(0, 10))
        .stream()
        .map(userActivityMapper::toArticleViewDto)
        .toList();

    log.info("활동 내역 조회 완료: userId={}", userId);

    return new UserActivityResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt(),
        subscriptionActivityResponses,
        commentActivityResponses,
        commentLikeActivityResponses,
        articleViewActivityResponses
    );
  }
}