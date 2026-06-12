package com.sprint.mission.monew.domain.useractivity.service;

import com.sprint.mission.monew.domain.user.exception.UserAccessDeniedException;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.listener.ArticleViewDeletedEvent;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class UserActivityService {

  private final UserRepository userRepository;
  private final UserActivityMongoRepository userActivityMongoRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void deleteArticleView(UUID userId, UUID articleId, UUID requestUserId) {
    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }
    userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    eventPublisher.publishEvent(new ArticleViewDeletedEvent(userId, articleId));
  }

  public UserActivity getUserActivity(UUID userId, UUID requestUserId) {
    log.debug("활동 내역 조회 시도: userId={}", userId);

    if (!userId.equals(requestUserId)) {
      throw UserAccessDeniedException.forUser(requestUserId);
    }

    userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    UserActivity activity = userActivityMongoRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));

    log.info("활동 내역 조회 완료: userId={}", userId);

    return activity;
  }
}