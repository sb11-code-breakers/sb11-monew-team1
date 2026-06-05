package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.entity.AccountUnlockToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUnlockTokenRepository extends JpaRepository<AccountUnlockToken, UUID> {

  Optional<AccountUnlockToken> findByTokenAndExpiredAtAfter(String token, Instant now);

  void deleteByUserId(UUID userId);
}