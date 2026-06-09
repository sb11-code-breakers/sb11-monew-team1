package com.sprint.mission.monew.domain.notification.listener;

import com.sprint.mission.monew.domain.comment.event.CommentLikedNotificationEvent;
import com.sprint.mission.monew.domain.notification.metrics.NotificationMetrics;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;
  private final NotificationMetrics notificationMetrics;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLiked(CommentLikedNotificationEvent event) {
    try {
      notificationService.create(
          event.recipientId(), event.message(), event.resourceType(), event.resourceId());
    } catch (Exception e) {
      // 좋아요 트랜잭션은 이미 커밋된 상태이므로 알림 실패를 전파하지 않고 로깅·메트릭만 남긴다.
      notificationMetrics.countCommentLikeFailure();
      log.error(
          "좋아요 알림 생성 실패 | recipientId={}, resourceId={}",
          event.recipientId(),
          event.resourceId(),
          e);
    }
  }
}
