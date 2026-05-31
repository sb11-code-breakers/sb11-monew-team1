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
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;

  public CursorPageResponse<NotificationResponse> findUnconfirmed(
      UUID userId, NotificationQueryCondition condition) {
    return notificationRepository.findUnconfirmed(userId, condition);
  }

  @Transactional
  public void confirmAll(UUID userId) {
    notificationRepository.confirmAllByUserId(userId, Instant.now());
  }

  @Transactional
  public void deleteExpiredNotifications() {
    Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
    int deleted = notificationRepository.deleteConfirmedBefore(cutoff);
    log.info("만료 알림 삭제 완료: {}건", deleted);
  }

  @Transactional
  public void createCommentLikeNotification(
      UUID commentId, UUID commentAuthorId, String likerNickname) {
    Notification notification =
        Notification.create(
            commentAuthorId,
            "[" + likerNickname + "]님이 나의 댓글을 좋아합니다.",
            ResourceType.COMMENT,
            commentId);
    Notification saved = notificationRepository.save(notification);
    log.info("댓글 좋아요 알림 생성 완료: 알림 ID={}, 수신자={}", saved.getId(), commentAuthorId);
  }

  @Transactional
  public void createArticleNotifications(
      UUID interestId, String interestName, List<UUID> subscriberIds) {
    if (subscriberIds.isEmpty()) {
      return;
    }
    List<Notification> notifications =
        subscriberIds.stream()
            .map(
                uid ->
                    Notification.create(
                        uid,
                        "[" + interestName + "]와 관련된 기사가 등록되었습니다.",
                        ResourceType.INTEREST,
                        interestId))
            .toList();
    List<Notification> saved = notificationRepository.saveAll(notifications);
    log.info("기사 등록 알림 생성 완료: 관심사={}, 수신자={}명", interestName, saved.size());
  }

  @Transactional
  public void confirm(UUID notificationId, UUID userId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserIdAndConfirmedAtIsNull(notificationId, userId)
            .orElseThrow(() -> NotificationNotFoundException.withId(notificationId));
    notification.confirm();
  }
}
