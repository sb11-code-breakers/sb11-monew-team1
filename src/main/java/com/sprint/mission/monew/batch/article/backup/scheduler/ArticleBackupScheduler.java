package com.sprint.mission.monew.batch.article.backup.scheduler;

import com.sprint.mission.monew.batch.article.backup.service.ArticleBackupService;
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
public class ArticleBackupScheduler {

  private final ArticleBackupService articleBackupService;

  @Scheduled(cron = "${scheduler.article-backup.cron}", zone = "${scheduler.timezone}")
  public void executeBackup() throws Exception {
    log.info("기사 S3 백업 배치 시작");
    articleBackupService.executeBackup();
    log.info("기사 S3 백업 배치 완료");
  }
}