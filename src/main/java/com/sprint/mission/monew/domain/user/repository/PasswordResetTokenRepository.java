package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  Optional<PasswordResetToken> findByCodeAndExpiredAtAfter(String code, Instant now);

  void deleteByUserId(UUID userId);
}