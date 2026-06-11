package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.batch.user.cleanup.dto.UserCleanupItem;
import com.sprint.mission.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  Optional<User> findByEmailAndDeletedAtIsNull(String email);

  Optional<User> findByIdAndDeletedAtIsNull(UUID id);

  Optional<User> findByIdAndDeletedAtIsNotNull(UUID id);

  @Modifying
  @Query("DELETE FROM User u WHERE u.deletedAt < :threshold")
  int deleteAllByDeletedAtBefore(@Param("threshold") Instant threshold);

  @Query("""
      SELECT new com.sprint.mission.monew.batch.user.cleanup.dto.UserCleanupItem(
          u.id,
          u.deletedAt
      )
      FROM User u
      WHERE u.deletedAt < :threshold
      AND (
          u.deletedAt > :lastDeletedAt
          OR (u.deletedAt = :lastDeletedAt AND u.id > :lastId)
      )
      ORDER BY u.deletedAt ASC, u.id ASC
      """)
  List<UserCleanupItem> findUsersForCleanup(
      @Param("threshold") Instant threshold,
      @Param("lastDeletedAt") Instant lastDeletedAt,
      @Param("lastId") UUID lastId,
      Pageable pageable
  );
}