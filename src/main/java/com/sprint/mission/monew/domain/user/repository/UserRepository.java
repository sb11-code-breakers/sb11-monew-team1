package com.sprint.mission.monew.domain.user.repository;

import com.sprint.mission.monew.domain.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  boolean existsByEmail(String email);

  Optional<User> findByEmailAndDeletedAtIsNull(String email);

  Optional<User> findByIdAndDeletedAtIsNull(UUID id);

  @Modifying
  @Query("DELETE FROM User u WHERE u.deletedAt < :threshold")
  int deleteAllByDeletedAtBefore(@Param("threshold") Instant threshold);
}