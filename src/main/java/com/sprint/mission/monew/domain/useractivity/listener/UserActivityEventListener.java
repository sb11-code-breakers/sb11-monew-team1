package com.sprint.mission.monew.domain.useractivity.listener;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityEventListener {

  private final UserActivityMongoRepository userActivityMongoRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserCreatedEvent event) {
    log.debug("UserActivity 생성 | userId={}", event.userId());
    UserActivity userActivity = UserActivity.of(event.userId(), event.email(), event.nickname(), event.createdAt());
    userActivityMongoRepository.createUserActivity(userActivity);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserDeletedEvent event) {
    log.debug("UserActivity 익명화 | userId={}", event.userId());
    userActivityMongoRepository.anonymize(event.userId());
    userActivityMongoRepository.anonymizeCommentLikesByCommentUserId(event.userId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserNicknameUpdatedEvent event) {
    log.debug("UserActivity 닉네임 업데이트 | userId={}", event.userId());
    userActivityMongoRepository.updateNickname(event.userId(), event.nickname());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCreatedEvent event) {
    log.debug("구독 push | userId={}, interestId={}", event.userId(), event.interestId());
    RecentSubscription subscription = RecentSubscription.of(
        event.subscriptionId(),
        event.interestId(),
        event.interestName(),
        event.interestKeywords(),
        event.interestSubscriberCount(),
        event.createdAt()
    );
    userActivityMongoRepository.pushSubscription(event.userId(), subscription);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SubscriptionCancelledEvent event) {
    log.debug("구독 pull | userId={}, interestId={}", event.userId(), event.interestId());
    userActivityMongoRepository.pullSubscription(event.userId(), event.interestId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentCreatedEvent event) {
    log.debug("댓글 push | userId={}, commentId={}", event.userId(), event.commentId());
    RecentComment comment = RecentComment.of(
        event.commentId(), event.articleId(), event.articleTitle(),
        event.userId(), event.userNickname(), event.content(), event.likeCount(), event.createdAt()
    );
    userActivityMongoRepository.pushComment(event.userId(), comment);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentUpdatedEvent event) {
    log.debug("댓글 content 업데이트 | commentId={}", event.commentId());
    userActivityMongoRepository.updateCommentContent(event.commentId(), event.content());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentDeletedEvent event) {
    log.debug("댓글 pull | authorId={}, commentId={}", event.authorId(), event.commentId());
    userActivityMongoRepository.pullComment(event.authorId(), event.commentId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikedEvent event) {
    log.debug("댓글 좋아요 push | userId={}, commentId={}", event.userId(), event.commentId());
    RecentCommentLike commentLike = RecentCommentLike.of(
        event.likeId(), event.createdAt(),
        event.commentId(), event.articleId(), event.articleTitle(),
        event.commentUserId(), event.commentUserNickname(), event.commentContent(),
        event.commentLikeCount(), event.commentCreatedAt()
    );
    userActivityMongoRepository.pushCommentLike(event.userId(), commentLike);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(CommentLikeRemovedEvent event) {
    log.debug("댓글 좋아요 pull | userId={}, commentId={}", event.userId(), event.commentId());
    userActivityMongoRepository.pullCommentLike(event.userId(), event.commentId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleViewedEvent event) {
    log.debug("기사 조회 push | userId={}, articleId={}", event.userId(), event.articleId());
    RecentArticleView articleView = RecentArticleView.of(
        event.articleViewId(), event.userId(), event.createdAt(),
        event.articleId(), event.source(), event.sourceUrl(), event.articleTitle(),
        event.articlePublishedDate(), event.articleSummary(),
        event.articleCommentCount(), event.articleViewCount()
    );
    userActivityMongoRepository.pushArticleView(event.userId(), articleView);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ArticleDeletedEvent event) {
    log.debug("기사 삭제 cascade | articleId={}", event.articleId());
    userActivityMongoRepository.pullArticleViewsByArticleId(event.articleId());
    userActivityMongoRepository.pullCommentsByArticleId(event.articleId());
    userActivityMongoRepository.pullCommentLikesByArticleId(event.articleId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(InterestDeletedEvent event) {
    log.debug("관심사 삭제 cascade | interestId={}", event.interestId());
    userActivityMongoRepository.pullSubscriptionsByInterestId(event.interestId());
  }
}
