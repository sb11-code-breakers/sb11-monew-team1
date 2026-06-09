package com.sprint.mission.monew.domain.useractivity.listener;

import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.util.UUID;
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
    com.sprint.mission.monew.domain.useractivity.document.RecentSubscription subscription =
        com.sprint.mission.monew.domain.useractivity.document.RecentSubscription.of(
            UUID.randomUUID(), // 구독 활동 자체의 ID
            event.interestId(), event.interestName(), event.interestKeywords(),
            event.interestSubscriberCount(), event.createdAt()
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
            event.commentId(), event.articleId(), event.articleTitle(),
            event.userId(), event.userNickname(), event.content(),
            event.likeCount(), event.createdAt()
        );
    userActivityMongoRepository.pushComment(event.userId(),comment);

  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentUpdatedEvent event) {
    userActivityMongoRepository.updateCommentContent(event.userId(), event.commentId(), event.content());
  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikedEvent event) {
    com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike commentLike =
        com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike.of(
            event.likeId(), event.createdAt(), event.commentId(), event.articleId(),
            event.articleTitle(), event.commentUserId(), event.commentUserNickname(),
            event.commentContent(), event.commentLikeCount(), event.commentCreatedAt()
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
            event.viewId(), event.userId(), event.createdAt(), event.articleId(),
            event.source(), event.sourceUrl(), event.articleTitle(),
            event.articlePublishedDate(), event.articleSummary(),
            event.articleCommentCount(), event.articleViewCount()
        );
    userActivityMongoRepository.pushArticleView(event.userId(), articleView);
  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleDeletedEvent event) {
    userActivityMongoRepository.pullArticle(event.userId(), event.articleId());
    userActivityMongoRepository.pullArticleLike(event.userId(), event.articleId());
    userActivityMongoRepository.pullArticleView(event.userId(), event.articleId());
  }
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterestDeletedEvent event) {
    userActivityMongoRepository.pullSubscriptionsByInterestId(event.interestId());
  }
}