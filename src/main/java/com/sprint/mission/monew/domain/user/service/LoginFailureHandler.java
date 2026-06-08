package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler {

  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(UUID userId) {
    return userRepository.findById(userId).map(user -> {
      user.incrementLoginFailCount();
      if (user.hasExceededLoginFailLimit()) {
        user.lock();
        return true;
      }
      return false;
    }).orElse(false);
  }
}