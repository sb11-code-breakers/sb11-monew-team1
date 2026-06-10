package com.sprint.mission.monew.domain.useractivity.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.useractivity.document.RecentArticleView;
import com.sprint.mission.monew.domain.useractivity.document.RecentComment;
import com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike;
import com.sprint.mission.monew.domain.useractivity.document.RecentSubscription;
import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityEventListenerTest {

  @Mock
  UserActivityMongoRepository userActivityMongoRepository;

  @InjectMocks
  UserActivityEventListener listener;

  // 💡 리스너가 리포지토리로 넘기는 객체를 낚아채서(Capture) 안의 내용물을 검증하기 위한 도구들
  @Captor ArgumentCaptor<UserActivity> activityCaptor;
  @Captor ArgumentCaptor<RecentSubscription> subscriptionCaptor;
  @Captor ArgumentCaptor<RecentComment> commentCaptor;
  @Captor ArgumentCaptor<RecentCommentLike> commentLikeCaptor;
  @Captor ArgumentCaptor<RecentArticleView> articleViewCaptor;

  @Nested
  @DisplayName("UserCreatedEvent")
  class UserCreated {
    @Test
    @DisplayName("UserCreatedEvent를 받으면 UserActivity 도큐먼트 신규 생성 (ID, Timestamp)")
    void handle_UserCreated() {
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserActivity activity = UserActivity.of(userId, now);

      listener.handle(new UserCreatedEvent(activity));

      verify(userActivityMongoRepository).createUserActivity(activityCaptor.capture());
      UserActivity captured = activityCaptor.getValue();
      assertThat(captured.getId()).isEqualTo(userId);
    }
  }

  @Nested
  @DisplayName("UserDeletedEvent")
  class UserDeleted {
    @Test
    @DisplayName("UserDeletedEvent를 받으면 anonymize와 anonymizeCommentLikesByCommentUserId를 모두 호출한다")
    void handle_UserDeleted() {
      UUID userId = UUID.randomUUID();
      listener.handle(new UserDeletedEvent(userId));
      verify(userActivityMongoRepository).anonymize(userId);
      verify(userActivityMongoRepository).anonymizeCommentLikesByCommentUserId(userId);
    }
  }

  @Nested
  @DisplayName("SubscriptionCreatedEvent")
  class SubscriptionCreated {
    @Test
    @DisplayName("SubscriptionCreatedEvent를 받으면 {interestId, interestName, subscribedAt} 추출 및 push")
    void handle_SubscriptionCreated() {
      UUID userId = UUID.randomUUID();
      UUID interestId = UUID.randomUUID();
      Instant subscribedAt = Instant.now();
      String interestName = "IT 기술";

      SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(userId, interestId, interestName, subscribedAt);
      listener.handle(event);

      verify(userActivityMongoRepository).pushSubscription(eq(userId), subscriptionCaptor.capture());

      RecentSubscription captured = subscriptionCaptor.getValue();
      assertThat(captured.getInterestId()).isEqualTo(interestId);
      assertThat(captured.getInterestName()).isEqualTo(interestName);
      assertThat(captured.getSubscribedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("SubscriptionCancelledEvent")
  class SubscriptionCancelled {
    @Test
    @DisplayName("SubscriptionCancelledEvent를 받으면 pullSubscription을 호출한다")
    void handle_SubscriptionCancelled() {
      UUID userId = UUID.randomUUID();
      UUID targetId = UUID.randomUUID();
      listener.handle(new SubscriptionCancelledEvent(userId, targetId));
      verify(userActivityMongoRepository).pullSubscription(userId, targetId);
    }
  }

  @Nested
  @DisplayName("CommentCreatedEvent")
  class CommentCreated {
    @Test
    @DisplayName("CommentCreatedEvent를 받으면 {commentId, articleId, articleTitle, createdAt} 추출 및 push")
    void handle_CommentCreated() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      String articleTitle = "TDD 정석 가이드";
      Instant createdAt = Instant.now();

      CommentCreatedEvent event = new CommentCreatedEvent(userId, commentId, articleId, articleTitle, createdAt);
      listener.handle(event);

      verify(userActivityMongoRepository).pushComment(eq(userId), commentCaptor.capture());

      RecentComment captured = commentCaptor.getValue();
      assertThat(captured.getCommentId()).isEqualTo(commentId);
      assertThat(captured.getArticleId()).isEqualTo(articleId);
      assertThat(captured.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(captured.getCreatedAt()).isEqualTo(createdAt);
    }
  }

  @Nested
  @DisplayName("CommentLikedEvent")
  class CommentLiked {
    @Test
    @DisplayName("CommentLikedEvent를 받으면 {commentId, articleId, articleTitle, commentCreatedAt, likedAt} 추출 및 push")
    void handle_CommentLiked() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      String articleTitle = "클린 아키텍처";
      Instant commentCreatedAt = Instant.now().minusSeconds(3600);
      Instant likedAt = Instant.now();

      CommentLikedEvent event = new CommentLikedEvent(
          userId,                   // 1. userId
          UUID.randomUUID(),        // 2. likeId
          likedAt,                  // 3. createdAt (좋아요 누른 시간)
          commentId,                // 4. commentId
          articleId,                // 5. articleId
          articleTitle,             // 6. articleTitle
          commentCreatedAt          // 7. commentCreatedAt
      );

      listener.handle(event);

      verify(userActivityMongoRepository).pushCommentLike(eq(userId), commentLikeCaptor.capture());

      RecentCommentLike captured = commentLikeCaptor.getValue();
      assertThat(captured.getCommentId()).isEqualTo(commentId);
      assertThat(captured.getArticleId()).isEqualTo(articleId);
      assertThat(captured.getArticleTitle()).isEqualTo(articleTitle);
      assertThat(captured.getCommentCreatedAt()).isEqualTo(commentCreatedAt);
      assertThat(captured.getLikedAt()).isEqualTo(likedAt);
    }
  }

  @Nested
  @DisplayName("CommentLikeRemovedEvent")
  class CommentLikeRemoved {
    @Test
    @DisplayName("CommentLikeRemovedEvent를 받으면 pullCommentLike를 호출한다")
    void handle_CommentLikeRemoved() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      listener.handle(new CommentLikeRemovedEvent(userId, commentId));
      verify(userActivityMongoRepository).pullCommentLike(userId, commentId);
    }
  }

  @Nested
  @DisplayName("ArticleViewedEvent")
  class ArticleViewed {
    @Test
    @DisplayName("ArticleViewedEvent를 받으면 불변 필드 전체 추출 및 push")
    void handle_ArticleViewed() {
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      Instant viewTime = Instant.now();

      ArticleViewedEvent event = new ArticleViewedEvent(
          userId, UUID.randomUUID(), viewTime, articleId,
          "NAVER", "https://url", "오늘의 뉴스", Instant.now(), "기사 요약"
      );

      listener.handle(event);

      verify(userActivityMongoRepository).pushArticleView(eq(userId), articleViewCaptor.capture());

      RecentArticleView captured = articleViewCaptor.getValue();
      assertThat(captured.getArticleId()).isEqualTo(articleId);
      assertThat(captured.getArticleTitle()).isEqualTo("오늘의 뉴스");
      assertThat(captured.getSource()).isEqualTo("NAVER");
      assertThat(captured.getViewedAt()).isEqualTo(viewTime);
    }
  }

  @Nested
  @DisplayName("ArticleDeletedEvent")
  class ArticleDeleted {
    @Test
    @DisplayName("ArticleDeletedEvent를 받으면 연쇄 삭제(Cascade) pull 3개 메서드를 호출한다")
    void handle_ArticleDeleted() {
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();
      listener.handle(new ArticleDeletedEvent(userId, articleId));

      verify(userActivityMongoRepository).pullArticleViewsByArticleId(articleId);
      verify(userActivityMongoRepository).pullCommentsByArticleId(articleId);
      verify(userActivityMongoRepository).pullCommentLikesByArticleId(articleId);
    }
  }

  @Nested
  @DisplayName("InterestDeletedEvent")
  class InterestDeleted {
    @Test
    @DisplayName("InterestDeletedEvent를 받으면 pullSubscriptionsByInterestId를 호출한다")
    void handle_InterestDeleted() {
      UUID interestId = UUID.randomUUID();
      listener.handle(new InterestDeletedEvent(interestId));
      verify(userActivityMongoRepository).pullSubscriptionsByInterestId(interestId);
    }
  }
}