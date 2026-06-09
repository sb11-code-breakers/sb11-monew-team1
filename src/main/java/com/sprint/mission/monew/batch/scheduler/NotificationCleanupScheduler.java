package com.sprint.mission.monew.batch.scheduler;

import com.sprint.mission.monew.batch.service.NotificationCleanupService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("prod")
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

  private final NotificationCleanupService notificationCleanupService;

  @Timed(value = "monew.notification.cleanup.job.duration", description = "만료 알림 정리 배치 Job 전체 소요 시간")
  @Scheduled(cron = "${scheduler.notification-cleanup.cron}")
  public void cleanUpExpiredNotifications() throws Exception {
    log.debug("만료 알림 삭제 스케줄러 실행");
    notificationCleanupService.executeCleanup();
    log.info("만료 알림 삭제 완료");
  }
}
