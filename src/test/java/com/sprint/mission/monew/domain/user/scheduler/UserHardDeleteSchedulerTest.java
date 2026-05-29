package com.sprint.mission.monew.domain.user.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.user.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserHardDeleteSchedulerTest {

  private UserHardDeleteScheduler scheduler;

  @Mock
  private UserService userService;

  @BeforeEach
  void setUp() {
    scheduler = new UserHardDeleteScheduler(userService);
  }

  @Test
  @DisplayName("스케줄러가 UserService의 물리 삭제 메서드를 호출한다")
  void 스케줄러가_UserService의_물리_삭제_메서드를_호출한다() {
    // when
    scheduler.cleanUpDeletedUsers();

    // then
    then(userService).should().deleteExpiredUsers(any(Instant.class));
  }
}