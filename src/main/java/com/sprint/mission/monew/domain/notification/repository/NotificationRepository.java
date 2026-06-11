package com.sprint.mission.monew.domain.notification.repository;

import com.sprint.mission.monew.batch.notification.cleanup.dto.NotificationCleanupItem;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.repository.querydsl.NotificationCustomRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationCustomRepository {

  Optional<Notification> findByIdAndUserIdAndConfirmedAtIsNull(UUID id, UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Notification n
         SET n.confirmedAt = :now
       WHERE n.userId = :userId
         AND n.confirmedAt IS NULL
      """)
  int confirmAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM Notification n WHERE n.confirmedAt < :cutoff")
  int deleteConfirmedBefore(@Param("cutoff") Instant cutoff);

  @Query("""
    SELECT new com.sprint.mission.monew.batch.notification.cleanup.dto.NotificationCleanupItem(
        n.id,
        n.confirmedAt
    )
    FROM Notification n
    WHERE n.confirmedAt < :cutoff
    AND (
        n.confirmedAt > :lastConfirmedAt
        OR (n.confirmedAt = :lastConfirmedAt AND n.id > :lastId)
    )
    ORDER BY n.confirmedAt ASC, n.id ASC
""")
  List<NotificationCleanupItem> findNotificationsForCleanup(
      @Param("cutoff") Instant cutoff,
      @Param("lastConfirmedAt") Instant lastConfirmedAt,
      @Param("lastId") UUID lastId,
      Pageable pageable
  );
}