package com.sprint.mission.monew.domain.useractivity.listener;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityMongoRepository userActivityMongoRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserCreatedEvent event) {
    // 💡 [수정] UserCreatedEvent가 순수 팩트(UUID userId, Instant createdAt) 구조로 다이어트됨에 따라
    // 더 이상 존재하지 않는 event.userActivity() 호출부 제거 후, 식별자를 통해 초기 빈 도큐먼트를 생성하도록 교정
    UserActivity userActivity = UserActivity.of(event.userId(),event.createdAt());
    userActivityMongoRepository.createUserActivity(userActivity);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserDeletedEvent event) {
    userActivityMongoRepository.anonymize(event.userId());
    userActivityMongoRepository.anonymizeCommentLikesByCommentUserId(event.userId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCreatedEvent event) {
    RecentSubscription subscription = RecentSubscription.of(
        event.interestId(),
        event.interestName(),
        event.createdAt()
    );
    userActivityMongoRepository.pushSubscription(event.userId(), subscription);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCancelledEvent event) {
    userActivityMongoRepository.pullSubscription(event.userId(), event.interestId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentCreatedEvent event) {
    RecentComment comment = RecentComment.of(
        event.commentId(), event.articleId(), event.articleTitle(), event.createdAt()
    );
    userActivityMongoRepository.pushComment(event.userId(), comment);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikedEvent event) {
    // RecentCommentLike.of에 commentCreatedAt을 추가로 전달
    RecentCommentLike commentLike = RecentCommentLike.of(
        event.commentId(),
        event.articleId(),
        event.articleTitle(),
        event.commentCreatedAt(),
        event.createdAt()
    );
    userActivityMongoRepository.pushCommentLike(event.userId(), commentLike);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikeRemovedEvent event) {
    userActivityMongoRepository.pullCommentLike(event.userId(), event.commentId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleViewedEvent event) {
    // 💡 [수정] 아래 첨부해주신 레코드 규격(createdAt이 2번째 순서)의 데이터 게터를 활용하여
    // RecentArticleView 생성 모델 내부 파라미터 매핑을 결함 없이 온전하게 유지합니다.
    RecentArticleView articleView = RecentArticleView.of(
        event.articleId(), event.source(), event.sourceUrl(), event.articleTitle(),
        event.articlePublishedDate(), event.articleSummary(), event.createdAt()
    );
    userActivityMongoRepository.pushArticleView(event.userId(), articleView);
  }

  // 💡 게시글 삭제 시 RDB 정합성을 위해 몽고DB 연쇄 삭제 로직으로 변경
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleDeletedEvent event) {
    userActivityMongoRepository.pullArticleViewsByArticleId(event.articleId());
    userActivityMongoRepository.pullCommentsByArticleId(event.articleId());
    userActivityMongoRepository.pullCommentLikesByArticleId(event.articleId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterestDeletedEvent event) {
    userActivityMongoRepository.pullSubscriptionsByInterestId(event.interestId());
  }
}