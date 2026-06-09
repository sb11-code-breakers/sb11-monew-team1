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
}