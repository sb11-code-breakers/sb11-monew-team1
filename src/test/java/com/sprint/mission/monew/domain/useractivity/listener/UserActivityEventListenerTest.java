package com.sprint.mission.monew.domain.useractivity.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityEventListenerTest {

  @Mock
  UserActivityMongoRepository userActivityMongoRepository;

  @InjectMocks
  UserActivityEventListener listener;

  @Nested
  @DisplayName("UserCreatedEvent")
  class UserCreated {
    @Test
    @DisplayName("UserCreatedEvent를 받으면 createUserActivity를 호출한다")
    void handle_UserCreated() {
      UserActivity activity = UserActivity.of(UUID.randomUUID(), Instant.now());
      listener.handle(new UserCreatedEvent(activity));
      verify(userActivityMongoRepository).createUserActivity(activity);
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
    @DisplayName("SubscriptionCreatedEvent를 받으면 pushSubscription을 호출한다")
    void handle_SubscriptionCreated() {
      // 이벤트 객체 스펙에 맞춰 더미 파라미터 모두 채움
      SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
          UUID.randomUUID(), UUID.randomUUID(), "IT", java.util.List.of("AI"), 42L, Instant.now()
      );
      listener.handle(event);
      verify(userActivityMongoRepository).pushSubscription(eq(event.userId()), org.mockito.ArgumentMatchers.any(com.sprint.mission.monew.domain.useractivity.document.RecentSubscription.class));
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
    @DisplayName("CommentCreatedEvent를 받으면 pushComment를 호출한다")
    void handle_CommentCreated() {
      CommentCreatedEvent event = new CommentCreatedEvent(
          UUID.randomUUID(),
          UUID.randomUUID(),
          UUID.randomUUID(),
          "기사 제목",
          Instant.now() // 파라미터 5개로 딱 맞춤!
      );
      listener.handle(event);
      verify(userActivityMongoRepository).pushComment(eq(event.userId()), org.mockito.ArgumentMatchers.any(com.sprint.mission.monew.domain.useractivity.document.RecentComment.class));
    }
  }

  @Nested
  @DisplayName("CommentLikedEvent")
  class CommentLiked {
    @Test
    @DisplayName("CommentLikedEvent를 받으면 pushCommentLike를 호출한다")
    void handle_CommentLiked() {
      CommentLikedEvent event = new CommentLikedEvent(
          UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
          UUID.randomUUID(), "기사 제목", UUID.randomUUID(), "작성자", "내용", 3L, Instant.now()
      );
      listener.handle(event);
      verify(userActivityMongoRepository).pushCommentLike(eq(event.userId()), org.mockito.ArgumentMatchers.any(com.sprint.mission.monew.domain.useractivity.document.RecentCommentLike.class));
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
    @DisplayName("ArticleViewedEvent를 받으면 pushArticleView를 호출한다")
    void handle_ArticleViewed() {
      ArticleViewedEvent event = new ArticleViewedEvent(
          UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
          "NAVER", "URL", "제목", Instant.now(), "요약", 10L, 20L
      );
      listener.handle(event);
      verify(userActivityMongoRepository).pushArticleView(eq(event.userId()), org.mockito.ArgumentMatchers.any(com.sprint.mission.monew.domain.useractivity.document.RecentArticleView.class));
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