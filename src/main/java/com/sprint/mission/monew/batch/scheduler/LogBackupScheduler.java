package com.sprint.mission.monew.batch.scheduler;

import com.sprint.mission.monew.batch.service.LogBackupService;
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
public class LogBackupScheduler {

  private final LogBackupService logBackupService;

  @Timed(value = "monew.log.upload.job.duration", description = "로그 백업 배치 Job 전체 소요 시간")
  @Scheduled(cron = "${scheduler.log-upload.cron}", zone = "${scheduler.timezone}")
  public void upload() throws Exception {
    log.info("로그 백업 배치 시작");
    logBackupService.executeBackup();
    log.info("로그 백업 배치 완료");
  }
}
