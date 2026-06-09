package com.sprint.mission.monew.domain.useractivity.listener;

import static org.mockito.Mockito.verify;

import com.sprint.mission.monew.domain.useractivity.document.UserActivity;
import com.sprint.mission.monew.domain.useractivity.repository.UserActivityMongoRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Nested;
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
      UUID userId = UUID.randomUUID();
      UUID targetId = UUID.randomUUID(); // 구독 대상 ID

      listener.handle(new SubscriptionCreatedEvent(userId, targetId));
      verify(userActivityMongoRepository).pushSubscription(userId, targetId);
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

      listener.handle(nSubscriptionCancelledEvent(userId, targetId));

      verify(userActivityMongoRepository).pullSubscription(userId, targetId);
    }
  }
}