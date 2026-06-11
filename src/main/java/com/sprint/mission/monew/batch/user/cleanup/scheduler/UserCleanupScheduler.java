package com.sprint.mission.monew.batch.user.cleanup.scheduler;

import com.sprint.mission.monew.batch.user.cleanup.service.UserCleanupService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

  private final UserCleanupService userCleanupService;

  @Timed(value = "monew.user.cleanup.job.duration", description = "만료 사용자 물리 삭제 배치 Job 전체 소요 시간")
  @Scheduled(cron = "${scheduler.user-cleanup.cron}", zone = "${scheduler.timezone}")
  public void cleanUpDeletedUsers() throws Exception {
    log.debug("물리 삭제 스케줄러 실행");
    userCleanupService.executeCleanup();
    log.info("물리 삭제 완료");
  }
}
