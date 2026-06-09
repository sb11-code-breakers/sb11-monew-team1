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
    void UserCreatedEvent를_받으면_createUserActivity를_호출한다() {
      UserActivity activity = UserActivity.of(UUID.randomUUID(), "a@b.com", "닉", Instant.now());
      listener.handle(new UserCreatedEvent(activity));
      verify(userActivityMongoRepository).createUserActivity(activity);
    }
  }
  @Nested
  @DisplayName("UserNicknameUpdatedEvent")
  class UserNicknameUpdated {
    @Test
    @DisplayName("UserNicknameUpdatedEvent를 받으면 updateNickname을 호출한다")
    void UserNicknameUpdatedEvent를_받으면_updateNickname을_호출한다() {
      UUID userId = UUID.randomUUID();
      listener.handle(new UserNicknameUpdatedEvent(userId, "새닉네임"));
      verify(userActivityMongoRepository).updateNickname(userId, "새닉네임");
    }
  }
  @Nested
  @DisplayName("UserDeletedEvent")
  class UserDeleted {
    @Test
    @DisplayName("UserDeletedEvent를 받으면 anonymize와 anonymizeCommentLikesByCommentUserId를 모두 호출한다")
    void UserDeletedEvent를_받으면_anonymize와_anonymizeCommentLikes를_모두_호출한다() {
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
    void SubscriptionCreatedEvent를_받으면_pushSubscription을_호출한다() {
      SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
          UUID.randomUUID(), UUID.randomUUID(), "IT",
          java.util.List.of("AI"), 42L, Instant.now()
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
    void SubscriptionCancelledEvent를_받으면_pullSubscription을_호출한다() {
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
    void CommentCreatedEvent를_받으면_pushComment를_호출한다() {
      // given: 살이 꽉 찬 이벤트 발행
      CommentCreatedEvent event = new CommentCreatedEvent(
          UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
          "기사 제목", "작성자닉네임", "댓글 내용", 0L, Instant.now()
      );

      // when
      listener.handle(event);

      // then
      verify(userActivityMongoRepository).pushComment(eq(event.userId()), org.mockito.ArgumentMatchers.any(com.sprint.mission.monew.domain.useractivity.document.RecentComment.class));
    }
  }
  @Nested
  @DisplayName("CommentUpdatedEvent")
  class CommentUpdated {
    @Test
    @DisplayName("CommentUpdatedEvent를 받으면 updateCommentContent를 호출한다")
    void CommentUpdatedEvent를_받으면_updateCommentContent를_호출한다() {
      UUID userId = UUID.randomUUID();
      UUID commentId = UUID.randomUUID();
      String content = "수정된 댓글 내용";

      listener.handle(new CommentUpdatedEvent(userId, commentId, content));

      verify(userActivityMongoRepository).updateCommentContent(userId, commentId, content);
    }
  }
  @Nested
  @DisplayName("CommentLikedEvent")
  class CommentLiked {
    @Test
    @DisplayName("CommentLikedEvent를 받으면 pushCommentLike를 호출한다")
    void CommentLikedEvent를_받으면_pushCommentLike를_호출한다() {
      CommentLikedEvent event = new CommentLikedEvent(
          UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
          UUID.randomUUID(), "기사 제목", UUID.randomUUID(), "댓글작성자", "내용", 3L, Instant.now()
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
    void CommentLikeRemovedEvent를_받으면_pullCommentLike를_호출한다() {
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
    void ArticleViewedEvent를_받으면_pushArticleView를_호출한다() {
      ArticleViewedEvent event = new ArticleViewedEvent(
          UUID.randomUUID(), UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
          "NAVER", "URL", "제목", Instant.now(), "요약", 10L, 200L
      );
      listener.handle(event);
      verify(userActivityMongoRepository).pushArticleView(eq(event.userId()), org.mockito.ArgumentMatchers.any(com.sprint.mission.monew.domain.useractivity.document.RecentArticleView.class));
    }
  }
  @Nested
  @DisplayName("ArticleDeletedEvent")
  class ArticleDeleted {
    @Test
    @DisplayName("ArticleDeletedEvent를 받으면 pull 3개 메서드를 모두 호출한다")
    void ArticleDeletedEvent를_받으면_pull_3개_메서드를_모두_호출한다() {
      UUID userId = UUID.randomUUID();
      UUID articleId = UUID.randomUUID();

      listener.handle(new ArticleDeletedEvent(userId, articleId));

      verify(userActivityMongoRepository).pullArticle(userId, articleId);
      verify(userActivityMongoRepository).pullArticleLike(userId, articleId);
      verify(userActivityMongoRepository).pullArticleView(userId, articleId);
    }
  }
  @Nested
  @DisplayName("InterestDeletedEvent")
  class InterestDeleted {
    @Test
    @DisplayName("InterestDeletedEvent를 받으면 pullSubscriptionsByInterestId를 호출한다")
    void InterestDeletedEvent를_받으면_pullSubscriptionsByInterestId를_호출한다() {
      UUID interestId = UUID.randomUUID();

      listener.handle(new InterestDeletedEvent(interestId));

      verify(userActivityMongoRepository).pullSubscriptionsByInterestId(interestId);
    }
  }

}