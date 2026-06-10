package com.sprint.mission.monew.batch.scheduler;

import com.sprint.mission.monew.batch.service.ArticleBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("prod")
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBackupScheduler {

  private final ArticleBackupService articleBackupService;

  @Scheduled(cron = "${scheduler.article-backup.cron}", zone = "${scheduler.timezone}")
  public void backup() {
    try {
      log.info("기사 S3 백업 배치 시작");
      articleBackupService.backup();
      log.info("기사 S3 백업 배치 완료");
    } catch (Exception e) {
      log.error("기사 S3 백업 배치 실패", e);
    }
  }
}
