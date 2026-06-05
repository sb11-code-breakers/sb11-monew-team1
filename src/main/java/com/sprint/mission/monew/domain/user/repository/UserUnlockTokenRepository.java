package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.entity.UserUnlockToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserUnlockTokenRepository extends JpaRepository<UserUnlockToken, UUID> {

  Optional<UserUnlockToken> findByTokenAndExpiredAtAfter(String token, Instant now);

  void deleteByUserId(UUID userId);
}