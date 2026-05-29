package com.sprint.mission.monew.domain.user.scheduler;

import com.sprint.mission.monew.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserCleanupScheduler {

  private final UserService userService;

  @Scheduled(cron = "${scheduler.user-cleanup.cron}")
  public void cleanUpDeletedUsers() {
    log.debug("물리 삭제 스케줄러 실행");
    userService.deleteExpiredUsers();
    log.info("물리 삭제 완료");
  }
}