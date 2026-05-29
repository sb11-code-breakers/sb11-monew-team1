package com.sprint.mission.monew.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotificationTest {

  private UUID userId;
  private String content;
  private ResourceType resourceType;
  private UUID resourceId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    content = "AI와 관련된 기사가 3건 등록되었습니다.";
    resourceType = ResourceType.INTEREST;
    resourceId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("정적 팩토리 메서드")
  class Create {

    @Test
    @DisplayName("필수 정보를 전달하면 알림이 정상 생성된다")
    void 필수_정보를_전달하면_알림이_정상_생성된다() {
      // when
      Notification notification = Notification.create(userId, content, resourceType, resourceId);

      // then
      assertThat(notification.getUserId()).isEqualTo(userId);
      assertThat(notification.getContent()).isEqualTo(content);
      assertThat(notification.getResourceType()).isEqualTo(resourceType);
      assertThat(notification.getResourceId()).isEqualTo(resourceId);
      assertThat(notification.isConfirmed()).isFalse();
    }
  }

  @Nested
  @DisplayName("confirm")
  class Confirm {

    @Test
    @DisplayName("미확인 알림을 확인하면 확인 상태가 된다")
    void 미확인_알림을_확인하면_확인_상태가_된다() {
      // given
      Notification notification = Notification.create(userId, content, resourceType, resourceId);

      // when
      notification.confirm();

      // then
      assertThat(notification.isConfirmed()).isTrue();
    }

    @Test
    @DisplayName("이미 확인된 알림을 재확인해도 confirmedAt이 변경되지 않는다")
    void 이미_확인된_알림을_재확인해도_confirmedAt이_변경되지_않는다() {
      // given
      Notification notification = Notification.create(userId, content, resourceType, resourceId);
      notification.confirm();
      var firstConfirmedAt = notification.getConfirmedAt();

      // when
      notification.confirm();

      // then
      assertThat(notification.getConfirmedAt()).isEqualTo(firstConfirmedAt);
    }
  }
}