package com.sprint.mission.monew.batch.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.mission.monew.domain.user.metrics.UserMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

@ExtendWith(MockitoExtension.class)
public class UserCleanupStepListenerTest {

  @Mock
  UserMetrics userMetrics;

  @InjectMocks
  UserCleanupStepListener listener;

  @Mock
  StepExecution stepExecution;

  @Test
  @DisplayName("StepExecution writeCount를 metrics로 전달한다")
  void step_listener_metrics_test() {

    // given
    given(stepExecution.getWriteCount()).willReturn(123L);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(userMetrics).should().countDeleted(123);
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }
}