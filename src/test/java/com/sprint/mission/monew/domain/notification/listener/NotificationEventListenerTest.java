package com.sprint.mission.monew.domain.notification.listener;

import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.comment.event.CommentLikedNotificationEvent;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.notification.service.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

  @InjectMocks NotificationEventListener notificationEventListener;

  @Mock NotificationService notificationService;

  @Nested
  @DisplayName("CommentLikedNotificationEvent 처리")
  class HandleCommentLikedNotification {

    @Test
    @DisplayName("수신자와 메시지가 포함된 이벤트를 받아 저장만 위임한다")
    void 수신자와_메시지가_포함된_이벤트를_받아_저장만_위임한다() {
      // given
      UUID recipientId = UUID.randomUUID();
      String message = "[홍길동]님이 나의 댓글을 좋아합니다.";
      UUID resourceId = UUID.randomUUID();
      CommentLikedNotificationEvent event =
          new CommentLikedNotificationEvent(recipientId, message, ResourceType.COMMENT, resourceId);

      // when
      notificationEventListener.handleCommentLiked(event);

      // then
      then(notificationService)
          .should()
          .create(recipientId, message, ResourceType.COMMENT, resourceId);
    }
  }
}