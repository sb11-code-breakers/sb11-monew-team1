package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.article.repository.ArticleViewRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentLikeRepository;
import com.sprint.mission.monew.domain.comment.repository.CommentRepository;
import com.sprint.mission.monew.domain.interest.repository.SubscriptionRepository;
import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.dto.ArticleViewDto;
import com.sprint.mission.monew.domain.useractivity.dto.CommentDto;
import com.sprint.mission.monew.domain.useractivity.dto.CommentLikeDto;
import com.sprint.mission.monew.domain.useractivity.dto.SubscriptionDto;
import com.sprint.mission.monew.domain.useractivity.dto.UserActivityResponse;
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

  public UserActivityResponse getUserActivity(UUID userId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    List<SubscriptionDto> subscriptionDtos = subscriptionRepository
        .findByUserId(userId, PageRequest.of(0, 10))
        .stream()
        .map(userActivityMapper::toSubscriptionDto)
        .toList();

    List<CommentDto> commentDtos = commentRepository
        .findTop10RecentCommentsByUserId(userId, PageRequest.of(0, 10))
        .stream()
        .map(userActivityMapper::toCommentDto)
        .toList();

    List<CommentLikeDto> commentLikeDtos = commentLikeRepository
        .findTop10ByUserId(userId)
        .stream()
        .map(userActivityMapper::toCommentLikeDto)
        .toList();

    List<ArticleViewDto> articleViewDtos = articleViewRepository
        .findTop10ByUserIdAndArticleNotDeleted(userId)
        .stream()
        .map(userActivityMapper::toArticleViewDto)
        .toList();

    log.info("활동 내역 조회 완료: userId={}", userId);

    return new UserActivityResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getCreatedAt(),
        subscriptionDtos,
        commentDtos,
        commentLikeDtos,
        articleViewDtos
    );
  }
}