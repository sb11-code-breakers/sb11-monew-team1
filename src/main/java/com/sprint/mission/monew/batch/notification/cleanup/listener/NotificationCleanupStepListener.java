package com.sprint.mission.monew.batch.notification.cleanup.listener;

import com.sprint.mission.monew.domain.notification.metrics.NotificationMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupStepListener implements StepExecutionListener {

  private final NotificationMetrics notificationMetrics;

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {

    long deleted = stepExecution.getWriteCount();

    try {
      notificationMetrics.countDeleted(deleted);
    } catch (Exception e) {
      log.warn("메트릭 기록 실패 (배치는 계속 진행)", e);
    }

    log.info("Notification Cleanup Step 완료 | deleted={}", deleted);

    return stepExecution.getExitStatus();
  }
}
