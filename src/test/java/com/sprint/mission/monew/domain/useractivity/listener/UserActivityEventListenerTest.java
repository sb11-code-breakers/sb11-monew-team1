package com.sprint.mission.monew.domain.useractivity.listener;

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
  private UserActivityMongoRepository userActivityMongoRepository;

  @InjectMocks
  private UserActivityEventListener listener;

  @Nested
  @DisplayName("UserCreatedEvent")
  class UserCreated {

    @Test
    @DisplayName("UserCreatedEvent를 받으면 createUserActivity를 호출한다")
    void UserCreatedEvent를_받으면_createUserActivity를_호출한다() {
      // given
      UserActivity activity = UserActivity.of(UUID.randomUUID(), "a@b.com", "닉", Instant.now());
      UserCreatedEvent event = new UserCreatedEvent(activity);

      // when
      listener.handle(event);

      // then
      verify(userActivityMongoRepository).createUserActivity(activity);
    }
  }
}