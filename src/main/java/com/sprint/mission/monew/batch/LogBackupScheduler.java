package com.sprint.mission.monew.batch;

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

  @Scheduled(cron = "${scheduler.log-upload.cron}")
  public void uploadLogs() {
    logBackupService.upload();
  }
}
