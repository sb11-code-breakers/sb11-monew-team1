package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.document.EmailVerification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailVerificationRepository extends MongoRepository<EmailVerification, UUID> {
  Optional<EmailVerification> findByTokenAndExpiredAtAfter(String token, Instant now);
}