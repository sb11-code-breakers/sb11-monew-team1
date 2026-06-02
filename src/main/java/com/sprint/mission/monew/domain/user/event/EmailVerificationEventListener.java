package com.sprint.mission.monew.domain.user.event;

import com.sprint.mission.monew.domain.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationEventListener {

  private final EmailService emailService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleEmailVerificationCreated(EmailVerificationCreatedEvent event) {
    log.debug("이메일 인증 이벤트 수신");
    emailService.sendVerificationEmail(event.email(), event.token());
  }
}