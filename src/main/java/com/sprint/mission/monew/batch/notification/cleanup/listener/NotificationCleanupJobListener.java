package com.sprint.mission.monew.batch.notification.cleanup.listener;

import com.sprint.mission.monew.domain.notification.metrics.NotificationMetrics;
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
public class NotificationCleanupJobListener implements JobExecutionListener {

  private final NotificationMetrics notificationMetrics;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("Notification Cleanup Job 시작 | jobId={}, params={}",
        jobExecution.getId(),
        jobExecution.getJobParameters());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

    // Job 상태 판단 - 성공
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info("Notification Cleanup Job 성공 | jobId={}", jobExecution.getId());
      notificationMetrics.markSuccess();
    }

    // Job 상태 판단 - 실패
    else if (jobExecution.getStatus() == BatchStatus.FAILED) {
      log.error("Notification Cleanup Job 실패 | jobId={}, exitStatus={}",
          jobExecution.getId(),
          jobExecution.getExitStatus());
    }

    // Job 실행 시간
    if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
      Duration duration = Duration.between(
          jobExecution.getStartTime(),
          jobExecution.getEndTime()
      );

      notificationMetrics.recordJobDuration(duration);

      log.info("Notification Cleanup Job duration={}", duration);
    } else {
      log.warn("Notification Cleanup Job 시간 정보 누락 | start={}, end={}",
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
