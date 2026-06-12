package com.sprint.mission.monew.batch.comment.cleanup.scheduler;

import com.sprint.mission.monew.batch.comment.cleanup.service.CommentCleanupService;
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

  @Scheduled(cron = "${scheduler.comment-cleanup.cron}", zone = "${scheduler.timezone}")
  public void cleanUpDeletedComments() throws Exception {
    log.debug("만료 댓글 물리 삭제 스케줄러 실행");
    commentCleanupService.executeCleanup();
    log.info("만료 댓글 물리 삭제 완료");
  }
}
