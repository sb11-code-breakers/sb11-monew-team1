package com.sprint.mission.monew.batch.news.collect.listener;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
import com.sprint.mission.monew.domain.article.service.ArticleNotificationService;
import java.time.Duration;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectJobListener implements JobExecutionListener {

  private final NewsCollectMetrics newsCollectMetrics;
  private final ArticleNotificationService articleNotificationService;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("News Collect Job 시작 | jobId={}, params={}",
        jobExecution.getId(),
        jobExecution.getJobParameters());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {

    // Job 상태 판단 - 성공
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      log.info("News Collect Job 성공 | jobId={}", jobExecution.getId());
      newsCollectMetrics.markSuccess();
      // 전체 성공 시 알림 발행 시작
      try {
        articleNotificationService.notifyNewArticles(jobExecution.getCreateTime().toInstant(ZoneOffset.UTC));
      } catch (Exception e) {
        log.error("기사 알림 생성 실패", e);
      }
    }

    // Job 상태 판단 - 실패
    else if (jobExecution.getStatus() == BatchStatus.FAILED) {
      log.error("News Collect Job 실패 | jobId={}, exitStatus={}",
          jobExecution.getId(),
          jobExecution.getExitStatus());
    }

    // Job 실행 시간
    if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
      Duration duration = Duration.between(
          jobExecution.getStartTime(),
          jobExecution.getEndTime()
      );

      newsCollectMetrics.recordJobDuration(duration);

      log.info("News Collect Job duration={}", duration);
    } else {
      log.warn("News Collect Job 시간 정보 누락 | start={}, end={}",
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
