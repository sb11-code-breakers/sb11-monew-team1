package com.sprint.mission.monew.domain.interest.repository;

import com.sprint.mission.monew.domain.interest.entity.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByInterestIdAndUserId(UUID interestId, UUID userId);

  Optional<Subscription> findByInterestIdAndUserId(UUID interestId, UUID userId);
}