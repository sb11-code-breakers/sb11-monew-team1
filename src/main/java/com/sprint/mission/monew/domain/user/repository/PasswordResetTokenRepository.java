package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.document.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findByCodeAndExpiredAtAfter(String code, Instant now);
  void deleteByUserId(UUID userId);
}