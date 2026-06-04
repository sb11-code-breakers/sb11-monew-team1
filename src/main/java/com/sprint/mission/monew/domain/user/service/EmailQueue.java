package com.sprint.mission.monew.domain.user.service;

import com.sprint.mission.monew.common.util.MonewUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailQueue {

  private final LinkedBlockingQueue<EmailTask> queue = new LinkedBlockingQueue<>();
  private final EmailService emailService;

  public void enqueue(String email, String token) {
    queue.offer(new EmailTask(email, token));
    log.debug("이메일 큐 등록: to={}", MonewUtil.maskEmail(email));
  }

  @Scheduled(fixedDelay = 500)
  public void processQueue() {
    List<EmailTask> failedTasks = new ArrayList<>();
    EmailTask task;
    while ((task = queue.poll()) != null) {
      boolean success = emailService.sendVerificationEmail(task.email(), task.token());
      if (!success) {
        log.warn("이메일 발송 실패 (시도 {}회): to={}", task.retryCount() + 1,
            MonewUtil.maskEmail(task.email()));
        if (task.hasReachedMaxRetry()) {
          log.error("이메일 발송 최종 실패: to={}", MonewUtil.maskEmail(task.email()));
          continue;
        }
        failedTasks.add(task.incrementRetry());
      }
    }
    failedTasks.forEach(queue::offer);
  }
}