package com.sprint.mission.monew.batch.scheduler;

import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.batch.service.UserCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

  @Mock
  private UserCleanupService userCleanupService;

  @InjectMocks
  private UserCleanupScheduler scheduler;

  @Test
  @DisplayName("스케줄러가 Batch Job를 호출한다")
  void 스케줄러가_Batch_Job의_물리_삭제_메서드를_호출한다() throws Exception {
    // given

    // when
    scheduler.cleanUpDeletedUsers();

    // then
    then(userCleanupService).should().executeCleanup();
  }
}