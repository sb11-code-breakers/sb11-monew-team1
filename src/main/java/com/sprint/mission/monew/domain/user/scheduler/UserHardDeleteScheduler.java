package com.sprint.mission.monew.domain.user.scheduler;

import com.sprint.mission.monew.domain.user.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("prod")
@Slf4j
@RequiredArgsConstructor
@Component
public class UserHardDeleteScheduler {

  private final UserService userService;

  @Scheduled(cron = "${scheduler.user-cleanup.cron}")
  public void cleanUpDeletedUsers() {
    log.debug("물리 삭제 스케줄러 실행");
    Instant threshold = Instant.now().minus(1, ChronoUnit.DAYS);
    userService.deleteExpiredUsers(threshold);
    log.info("물리 삭제 완료");
  }
}