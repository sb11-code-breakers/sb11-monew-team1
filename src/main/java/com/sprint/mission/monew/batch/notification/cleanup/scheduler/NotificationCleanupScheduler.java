package com.sprint.mission.monew.batch.notification.cleanup.scheduler;

import com.sprint.mission.monew.batch.notification.cleanup.service.NotificationCleanupService;
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

  @Scheduled(cron = "${scheduler.notification-cleanup.cron}", zone = "${scheduler.timezone}")
  public void cleanUpExpiredNotifications() throws Exception {
    log.debug("만료 알림 삭제 스케줄러 실행");
    notificationCleanupService.executeCleanup();
    log.info("만료 알림 삭제 완료");
  }
}
