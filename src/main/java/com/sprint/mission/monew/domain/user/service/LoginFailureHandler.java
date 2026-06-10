package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@Component
@RequiredArgsConstructor
public class LoginFailureHandler {

  private final UserRepository userRepository;

  @Retryable(
      retryFor = ObjectOptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 50, multiplier = 2)
  )
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = false)
  public boolean handle(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> UserNotFoundException.withId(userId));
    user.incrementLoginFailCount();
    if (user.hasExceededLoginFailLimit()) {
      user.lock();
      return true;
    }
    return false;
  }

  @Recover
  public boolean recoverHandle(ObjectOptimisticLockingFailureException e, UUID userId) {
    log.warn("로그인 실패 처리 낙관적락 최종 실패 | userId={}", userId);
    throw e;
  }
}