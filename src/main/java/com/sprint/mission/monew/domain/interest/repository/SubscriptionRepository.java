package com.sprint.mission.monew.domain.interest.repository;

import com.sprint.mission.monew.domain.interest.entity.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByInterestIdAndUserId(UUID interestId, UUID userId);

  Optional<Subscription> findByInterestIdAndUserId(UUID interestId, UUID userId);

  @Query("SELECT s FROM Subscription s " +
      "JOIN FETCH s.interest i " +
      "WHERE s.user.id = :userId")
  List<Subscription> findAllByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query("SELECT s.user.id FROM Subscription s WHERE s.interest.id = :interestId")
  List<UUID> findUserIdsByInterestId(@Param("interestId") UUID interestId);

  @Query("SELECT s.interest.id, s.user.id FROM Subscription s WHERE s.interest.id IN :interestIds")
  List<Object[]> findUserIdsByInterestIds(@Param("interestIds") List<UUID> interestIds);
}