package com.sprint.mission.monew.domain.notification.scheduler;

import com.sprint.mission.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("prod")
@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationScheduler {

  private final NotificationService notificationService;

  @Scheduled(cron = "0 0 0 * * *")
  public void cleanUpExpiredNotifications() {
    log.debug("만료 알림 삭제 스케줄러 실행");
    notificationService.deleteExpiredNotifications();
    log.info("만료 알림 삭제 완료");
  }
}