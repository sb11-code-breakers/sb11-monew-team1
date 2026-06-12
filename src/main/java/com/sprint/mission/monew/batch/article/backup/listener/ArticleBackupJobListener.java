package com.sprint.mission.monew.batch.article.backup.listener;

import com.sprint.mission.monew.batch.article.backup.metrics.ArticleBackupMetrics;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleBackupJobListener implements JobExecutionListener {

  private final ArticleBackupMetrics articleBackupMetrics;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("Article Backup Job 시작 | jobId={}, params={}",
        jobExecution.getId(),
        jobExecution.getJobParameters());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

    // Job 상태 판단 - 성공
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info("Article Backup Job 성공 | jobId={}", jobExecution.getId());
      articleBackupMetrics.markSuccess();
    }

    // Job 상태 판단 - 실패
    else if (jobExecution.getStatus() == BatchStatus.FAILED) {
      log.error("Article Backup Job 실패 | jobId={}, exitStatus={}",
          jobExecution.getId(),
          jobExecution.getExitStatus());
    }

    // Job 실행 시간
    if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
      Duration duration = Duration.between(
          jobExecution.getStartTime(),
          jobExecution.getEndTime()
      );

      articleBackupMetrics.recordJobDuration(duration);

      log.info("Article Backup Job duration={}", duration);
    } else {
      log.warn("Article Backup Job 시간 정보 누락 | start={}, end={}",
          jobExecution.getStartTime(),
          jobExecution.getEndTime());
    }

    // Job이 실패했을 경우 원인 로그
    if (!jobExecution.getAllFailureExceptions().isEmpty()) {
      jobExecution.getAllFailureExceptions()
          .forEach(e -> log.error("Job 실패 원인 | ", e));
    }
  }
}
