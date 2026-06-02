package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.entity.EmailVerification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

  Optional<EmailVerification> findByTokenAndExpiredAtAfter(String token, Instant now);
}