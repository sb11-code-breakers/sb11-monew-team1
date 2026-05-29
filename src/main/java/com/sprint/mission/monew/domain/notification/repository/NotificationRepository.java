package com.sprint.mission.monew.domain.notification.repository;

import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.repository.querydsl.NotificationCustomRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationCustomRepository {

  // 조회해라 Id와 UserId를
  Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

  // count해라 userId가 일치하고 + confirmedAt이 NULL인 것을
  long countByUserIdAndConfirmedAtIsNull(UUID userId);

  // 특정 사용자의 미확인 알림 전체에 확인 시각을 지금으로 기록한다.
  // Query 어노테이션을 사용하면 메서드 시그니처 동작x, Query가 동작함
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Notification n
         SET n.confirmedAt = :now,
             n.updatedAt = :now
       WHERE n.userId = :userId
         AND n.confirmedAt IS NULL
      """)
  int confirmAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);


  // 알림 확인(confirmedAt)시간이 7일(threshold) 초과됐으면 삭제
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      DELETE FROM Notification n
       WHERE n.confirmedAt IS NOT NULL
         AND n.confirmedAt < :threshold
      """)
  int deleteConfirmedBefore(@Param("threshold") Instant threshold);
}