package com.sprint.mission.monew.batch.log.backup.listener;

import com.sprint.mission.monew.batch.log.backup.metrics.LogBackupMetrics;
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
public class LogBackupJobListener implements JobExecutionListener {

  private final LogBackupMetrics logBackupMetrics;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("Log Backup Job 시작 | jobId={}, params={}",
        jobExecution.getId(),
        jobExecution.getJobParameters());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

    // Job 상태 판단 - 성공
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info("Log Backup Job 성공 | jobId={}", jobExecution.getId());
      logBackupMetrics.markSuccess();
    }

    // Job 상태 판단 - 실패
    else if (jobExecution.getStatus() == BatchStatus.FAILED) {
      log.error("Log Backup Job 실패 | jobId={}, exitStatus={}",
          jobExecution.getId(),
          jobExecution.getExitStatus());
    }

    // Job 실행 시간
    if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
      Duration duration = Duration.between(
          jobExecution.getStartTime(),
          jobExecution.getEndTime()
      );

      logBackupMetrics.recordJobDuration(duration);

      log.info("Log Backup Job duration={}", duration);
    } else {
      log.warn("Log Backup Job 시간 정보 누락 | start={}, end={}",
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
