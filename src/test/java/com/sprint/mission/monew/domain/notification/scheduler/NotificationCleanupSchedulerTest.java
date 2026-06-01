package com.sprint.mission.monew.domain.notification.scheduler;

import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

  private NotificationCleanupScheduler scheduler;

  @Mock
  NotificationService notificationService;

  @BeforeEach
  void setUp() {
    scheduler = new NotificationCleanupScheduler(notificationService);
  }

  @Test
  @DisplayName("만료 알림 삭제 스케줄러 실행 시 서비스에 위임한다")
  void 만료_알림_삭제_스케줄러_실행_시_서비스에_위임한다() {
    // when
    scheduler.cleanUpExpiredNotifications();

    // then
    then(notificationService).should().deleteExpiredNotifications();
  }
}