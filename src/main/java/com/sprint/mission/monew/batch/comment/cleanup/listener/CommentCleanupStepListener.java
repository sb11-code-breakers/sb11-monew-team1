package com.sprint.mission.monew.batch.comment.cleanup.listener;

import com.sprint.mission.monew.batch.comment.cleanup.metrics.CommentCleanupMetrics;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentCleanupStepListener implements StepExecutionListener {

  private final CommentCleanupMetrics commentCleanupMetrics;

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {

    long deleted = stepExecution.getWriteCount();

    if (stepExecution.getStartTime() == null || stepExecution.getEndTime() == null) {
      log.warn("Comment Cleanup Step 시간 정보 누락 | startTime={}, endTime={}",
          stepExecution.getStartTime(), stepExecution.getEndTime());

      return stepExecution.getExitStatus();
    }

    Duration duration = Duration.between(
        stepExecution.getStartTime(),
        stepExecution.getEndTime()
    );

    try {
      commentCleanupMetrics.countDeleted(deleted);
      commentCleanupMetrics.recordStepDuration(duration);
    } catch (Exception e) {
      log.warn("메트릭 기록 실패 (배치는 계속 진행)", e);
    }

    log.info("Comment Cleanup Step 완료 | deleted={}", deleted);

    return stepExecution.getExitStatus();
  }
}
