package com.sprint.mission.monew.batch.news.collect.listener;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectStepListener implements StepExecutionListener {

  private final NewsCollectMetrics newsCollectMetrics;

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {

    if (stepExecution.getStartTime() == null || stepExecution.getEndTime() == null) {
      log.warn("News Collect Step 시간 정보 누락 | startTime={}, endTime={}",
          stepExecution.getStartTime(), stepExecution.getEndTime());

      return stepExecution.getExitStatus();
    }

    Duration duration = Duration.between(
        stepExecution.getStartTime(),
        stepExecution.getEndTime()
    );

    try {
      newsCollectMetrics.recordCollectDuration(duration);
    } catch (Exception e) {
      log.warn("메트릭 기록 실패 (배치는 계속 진행)", e);
    }

    log.info("News Collect Step 완료 | duration={}", duration);

    return stepExecution.getExitStatus();
  }
}
