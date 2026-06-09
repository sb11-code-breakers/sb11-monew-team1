package com.sprint.mission.monew.batch.listener;

import com.sprint.mission.monew.batch.metrics.CommentCleanupMetrics;
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

    commentCleanupMetrics.countDeleted(deleted);

    log.info("Comment Cleanup Step 완료 | deleted={}", deleted);

    return stepExecution.getExitStatus();
  }

}
