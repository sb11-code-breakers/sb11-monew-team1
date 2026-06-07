package com.sprint.mission.monew.batch.listener;

import com.sprint.mission.monew.domain.notification.service.NotificationMetrics;
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

    notificationMetrics.countDeleted(deleted);

    log.info("Notification Cleanup Step 완료 | deleted={}", deleted);

    return stepExecution.getExitStatus();
  }

}
