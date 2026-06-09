package com.sprint.mission.monew.batch.scheduler;

import com.sprint.mission.monew.batch.service.CommentCleanupService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class CommentCleanupScheduler {

  private final CommentCleanupService commentCleanupService;

  @Timed(value = "monew.comment.cleanup.job.duration", description = "만료 댓글 물리 삭제 배치 Job 전체 소요 시간")
  @Scheduled(cron = "${scheduler.comment-cleanup.cron}")
  public void cleanUpDeletedComments() throws Exception {
    log.debug("물리 삭제 스케줄러 실행");
    commentCleanupService.executeCleanup();
    log.info("물리 삭제 완료");
  }
}
