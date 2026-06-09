package com.sprint.mission.monew.domain.useractivity.listener;

import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityMongoRepository userActivityMongoRepository;

  // 원칙: PostgreSQL 커밋 성공 시 NoSQL 연쇄 처리를 위한 트랜잭션 격리 분리 선언 유지
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserCreatedEvent event) {
    userActivityMongoRepository.createUserActivity(event.userActivity());
  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserNicknameUpdatedEvent event) {
    userActivityMongoRepository.updateNickname(event.userId(), event.nickname());
  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserDeletedEvent event) {
    userActivityMongoRepository.anonymize(event.userId());
    userActivityMongoRepository.anonymizeCommentLikesByCommentUserId(event.userId());
  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCreatedEvent event) {
    userActivityMongoRepository.pushSubscription(event.userId(), event.targetId());
  }
}