package com.sprint.mission.monew.domain.user.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.user.service.UserService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

  private UserCleanupScheduler scheduler;

  @Mock
  private UserService userService;

  @BeforeEach
  void setUp() {
    scheduler = new UserCleanupScheduler(userService);
  }

  @Test
  @DisplayName("스케줄러가 UserService의 물리 삭제 메서드를 호출한다")
  void 스케줄러가_UserService의_물리_삭제_메서드를_호출한다() {
    // given
    Instant lowerBound = Instant.now().minus(1, ChronoUnit.DAYS).minusSeconds(1);
    Instant upperBound = Instant.now().minus(1, ChronoUnit.DAYS).plusSeconds(1);

    // when
    scheduler.cleanUpDeletedUsers();

    // then
    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
    then(userService).should().deleteExpiredUsers(captor.capture());
    assertThat(captor.getValue()).isBetween(lowerBound, upperBound);
  }
}