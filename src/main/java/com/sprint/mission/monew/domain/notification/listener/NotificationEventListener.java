package com.sprint.mission.monew.domain.notification.listener;

import com.sprint.mission.monew.domain.comment.event.CommentLikedNotificationEvent;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLiked(CommentLikedNotificationEvent event) {
    notificationService.create(
        event.recipientId(), event.message(), event.resourceType(), event.resourceId());
  }
}