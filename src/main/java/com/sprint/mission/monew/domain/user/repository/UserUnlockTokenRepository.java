package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.document.UserUnlockToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserUnlockTokenRepository extends MongoRepository<UserUnlockToken, UUID> {
  Optional<UserUnlockToken> findByTokenAndExpiredAtAfter(String token, Instant now);
  void deleteByUserId(UUID userId);
}