package com.sprint.mission.monew.domain.notification.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.sprint.mission.monew.domain.comment.event.CommentLikedNotificationEvent;
import com.sprint.mission.monew.domain.notification.entity.ResourceType;
import com.sprint.mission.monew.domain.notification.service.NotificationMetrics;
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

  @Mock NotificationMetrics notificationMetrics;

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

    @Test
    @DisplayName("알림 생성 실패 시 예외를 전파하지 않고 실패 메트릭을 증가시킨다")
    void 알림_생성_실패_시_예외를_전파하지_않고_실패_메트릭을_증가시킨다() {
      // given
      UUID recipientId = UUID.randomUUID();
      CommentLikedNotificationEvent event =
          new CommentLikedNotificationEvent(
              recipientId, "메시지", ResourceType.COMMENT, UUID.randomUUID());
      willThrow(new RuntimeException("DB 오류"))
          .given(notificationService)
          .create(recipientId, "메시지", ResourceType.COMMENT, event.resourceId());

      // when & then
      assertThatCode(() -> notificationEventListener.handleCommentLiked(event))
          .doesNotThrowAnyException();
      then(notificationMetrics).should().countCommentLikeFailure();
    }
  }
}