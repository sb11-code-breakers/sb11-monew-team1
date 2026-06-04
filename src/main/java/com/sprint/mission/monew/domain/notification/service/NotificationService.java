package com.sprint.mission.monew.domain.notification.service;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.entity.Notification;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.notification.exception.NotificationNotFoundException;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMetrics notificationMetrics;

  public CursorPageResponse<NotificationResponse> findUnconfirmed(
      UUID userId, NotificationQueryCondition condition) {
    return notificationRepository.findUnconfirmed(userId, condition);
  }

  @Transactional
  public void confirmAll(UUID userId) {
    notificationRepository.confirmAllByUserId(userId, Instant.now());
    log.info("알림 전체 확인 완료 | userId={}", userId);
  }

  @Transactional
  public void deleteExpiredNotifications() {
    Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
    int deleted = notificationRepository.deleteConfirmedBefore(cutoff);
    notificationMetrics.countDeleted(deleted);
    log.info("만료 알림 삭제 완료 | count={}", deleted);
  }

  @Transactional
  public void createArticleNotifications(UUID interestId, String message, List<UUID> subscriberIds) {
    if (subscriberIds.isEmpty()) {
      return;
    }
    List<Notification> notifications =
        subscriberIds.stream()
            .map(uid -> Notification.create(uid, message, ResourceType.INTEREST, interestId))
            .toList();
    List<Notification> saved = notificationRepository.saveAll(notifications);
    notificationMetrics.countArticleNotifications(saved.size());
    log.info("기사 등록 알림 생성 완료 | interestId={}, recipientCount={}", interestId, saved.size());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void create(UUID recipientId, String message, ResourceType resourceType, UUID resourceId) {
    Notification notification = Notification.create(recipientId, message, resourceType, resourceId);
    Notification saved = notificationRepository.save(notification);
    if (resourceType == ResourceType.COMMENT) {
      notificationMetrics.countCommentLikeNotification();
    } else {
      notificationMetrics.countArticleNotifications(1);
    }
    log.info("알림 생성 완료 | notificationId={}, recipientId={}", saved.getId(), recipientId);
  }

  @Transactional
  public void confirm(UUID notificationId, UUID userId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserIdAndConfirmedAtIsNull(notificationId, userId)
            .orElseThrow(() -> NotificationNotFoundException.withId(notificationId));
    notification.confirm();
    log.info("알림 확인 완료 | notificationId={}, userId={}", notificationId, userId);
  }
}
