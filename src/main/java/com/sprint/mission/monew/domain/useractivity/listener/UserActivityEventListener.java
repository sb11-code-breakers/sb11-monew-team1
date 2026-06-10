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

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserCreatedEvent event) {
    userActivityMongoRepository.createUserActivity(event.userActivity());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserDeletedEvent event) {
    userActivityMongoRepository.anonymize(event.userId());
    userActivityMongoRepository.anonymizeCommentLikesByCommentUserId(event.userId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCreatedEvent event) {
    com.sprint.mission.monew.domain.useractivity.document.RecentSubscription subscription =
        com.sprint.mission.monew.domain.useractivity.document.RecentSubscription.of(
            event.interestId(), event.interestName(), event.createdAt() // 구독일
        );
    userActivityMongoRepository.pushSubscription(event.userId(), subscription);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCancelledEvent event) {
    userActivityMongoRepository.pullSubscription(event.userId(), event.interestId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentCreatedEvent event) {
    com.sprint.mission.monew.domain.useractivity.document.RecentComment comment =
        com.sprint.mission.monew.domain.useractivity.document.RecentComment.of(
            event.commentId(), event.articleId(), event.articleTitle(), event.createdAt()
        );
    userActivityMongoRepository.pushComment(event.userId(), comment);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikedEvent event) {
    com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike commentLike =
        com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike.of(
            event.commentId(), event.articleId(), event.commentUserId(),
            event.articleTitle(), event.commentCreatedAt(), event.createdAt() // 좋아요 누른 시간
        );
    userActivityMongoRepository.pushCommentLike(event.userId(), commentLike);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikeRemovedEvent event) {
    userActivityMongoRepository.pullCommentLike(event.userId(), event.commentId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleViewedEvent event) {
    com.sprint.mission.monew.domain.useractivity.document.RecentArticleView articleView =
        com.sprint.mission.monew.domain.useractivity.document.RecentArticleView.of(
            event.articleId(), event.source(), event.sourceUrl(), event.articleTitle(),
            event.articlePublishedDate(), event.articleSummary(), event.createdAt() // 조회한 시간
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