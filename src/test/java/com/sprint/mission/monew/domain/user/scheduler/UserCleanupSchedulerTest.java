package com.sprint.mission.monew.domain.user.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

  @InjectMocks
  private UserCleanupScheduler scheduler;

  @Mock
  private UserRepository userRepository;

  @Test
  @DisplayName("논리 삭제 후 1일 경과한 사용자 물리 삭제")
  void 논리_삭제_후_1일_경과한_사용자_물리_삭제() {
    // when
    scheduler.cleanUpDeletedUsers();

    // then
    then(userRepository).should().deleteAllByDeletedAtBefore(any(Instant.class));
  }
}