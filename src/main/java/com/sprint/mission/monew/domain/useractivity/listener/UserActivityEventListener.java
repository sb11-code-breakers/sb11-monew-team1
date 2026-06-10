package com.sprint.mission.monew.domain.useractivity.listener;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
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
        event.commentCreatedAt(), // 💡 이벤트에서 받은 시간 전달
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