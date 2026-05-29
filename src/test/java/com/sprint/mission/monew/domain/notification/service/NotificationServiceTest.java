package com.sprint.mission.monew.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.common.dto.CursorPageResponse;
import com.sprint.mission.monew.domain.notification.dto.NotificationQueryCondition;
import com.sprint.mission.monew.domain.notification.dto.NotificationResponse;
import com.sprint.mission.monew.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @InjectMocks
  NotificationService notificationService;
  @Mock
  NotificationRepository notificationRepository;

  UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Nested
  @DisplayName("미확인 알림 목록 조회")
  class FindUnconfirmed {

    @Test
    @DisplayName("repository에 조회를 위임하고 결과를 반환한다")
    void repository에_조회를_위임하고_결과를_반환한다() {
      // given
      NotificationQueryCondition condition = new NotificationQueryCondition(null, null, 10);
      CursorPageResponse<NotificationResponse> expected =
          new CursorPageResponse<>(List.of(), null, null, false, 0, 0L);
      given(notificationRepository.findUnconfirmed(userId, condition)).willReturn(expected);

      // when
      CursorPageResponse<NotificationResponse> result =
          notificationService.findUnconfirmed(userId, condition);

      // then
      assertThat(result).isEqualTo(expected);
      then(notificationRepository).should().findUnconfirmed(userId, condition);
    }
  }
}