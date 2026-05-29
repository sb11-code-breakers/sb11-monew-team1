package com.sprint.mission.monew.domain.notification.repository;

import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.repository.querydsl.NotificationCustomRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationCustomRepository {

  Optional<Notification> findByIdAndUserIdAndConfirmedAtIsNull(UUID id, UUID userId);

  long countByUserIdAndConfirmedAtIsNull(UUID userId);
}