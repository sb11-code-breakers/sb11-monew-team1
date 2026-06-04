package com.sprint.mission.monew.domain.user.service;

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
    log.debug("이메일 큐 등록: to={}", maskEmail(email));
  }

  @Scheduled(fixedDelay = 500)
  public void processQueue() {
    EmailTask task;
    while ((task = queue.poll()) != null) {
      boolean success = emailService.sendVerificationEmail(task.email(), task.token());
      if (!success) {
        log.warn("이메일 발송 실패, 재적재: to={}", maskEmail(task.email()));
        queue.offer(task);
      }
    }
  }

  private String maskEmail(String email) {
    int atIndex = email.indexOf('@');
    if (atIndex <= 1) return "***";
    return email.charAt(0) + "***" + email.substring(atIndex);
  }
}