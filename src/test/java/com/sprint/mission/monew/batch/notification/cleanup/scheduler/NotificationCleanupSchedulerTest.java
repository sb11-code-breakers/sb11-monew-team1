package com.sprint.mission.monew.batch.notification.cleanup.scheduler;

import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.batch.notification.cleanup.service.NotificationCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

  @Mock
  private NotificationCleanupService notificationCleanupService;

  @InjectMocks
  private NotificationCleanupScheduler scheduler;

  @Test
  @DisplayName("스케줄러가 Batch Job을 호출한다")
  void 스케줄러가_Batch_Job의_만료_알림_삭제_메서드를_호출한다() throws Exception {
    // given

    // when
    scheduler.cleanUpExpiredNotifications();

    // then
    then(notificationCleanupService).should().executeCleanup();
  }
}