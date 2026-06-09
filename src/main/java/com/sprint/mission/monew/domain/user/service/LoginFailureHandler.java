package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.entity.User;
import com.sprint.mission.monew.domain.user.exception.UserNotFoundException;
import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler {

  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(UUID userId) {
    int maxRetry = 3;
    for (int i = 0; i < maxRetry; i++) {
      try {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.withId(userId));
        user.incrementLoginFailCount();
        if (user.hasExceededLoginFailLimit()) {
          user.lock();
          return true;
        }
        return false;
      } catch (ObjectOptimisticLockingFailureException e) {
        if (i == maxRetry - 1) {
          log.warn("로그인 실패 처리 낙관적락 최종 실패 | userId={}", userId);
          throw e;
        }
        log.warn("로그인 실패 처리 낙관적락 충돌, 재시도 {}/{} | userId={}", i + 1, maxRetry, userId);
      }
    }
    throw new ObjectOptimisticLockingFailureException(User.class, userId);
  }
}