package com.sprint.mission.monew.batch.comment.cleanup.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.sprint.mission.monew.batch.comment.cleanup.metrics.CommentCleanupMetrics;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

@ExtendWith(MockitoExtension.class)
public class CommentCleanupStepListenerTest {

  @Mock
  CommentCleanupMetrics commentCleanupMetrics;

  @InjectMocks
  CommentCleanupStepListener listener;

  @Mock
  StepExecution stepExecution;

  @Test
  @DisplayName("시작 시간 정보 없으면 metrics 호출 없이 종료한다")
  void 시간_null이면_metrics_미호출() {
    // given
    LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

    given(stepExecution.getStartTime()).willReturn(null);
    given(stepExecution.getEndTime()).willReturn(end);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(commentCleanupMetrics).shouldHaveNoInteractions();
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("완료 시간 정보 없으면 metrics 호출 없이 종료한다")
  void 완료_시간_null이면_metrics_미호출() {
    // given
    LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);

    given(stepExecution.getStartTime()).willReturn(start);
    given(stepExecution.getEndTime()).willReturn(null);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(commentCleanupMetrics).shouldHaveNoInteractions();
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("metrics 실패해도 Step은 정상 종료된다")
  void metrics_실패해도_Step_정상_종료() {
    // given
    LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
    LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

    given(stepExecution.getStartTime()).willReturn(start);
    given(stepExecution.getEndTime()).willReturn(end);
    given(stepExecution.getWriteCount()).willReturn(123L);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    doThrow(new RuntimeException("metrics fail")).when(commentCleanupMetrics)
        .countDeleted(anyLong());

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(commentCleanupMetrics).should().countDeleted(123L);
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("StepExecution writeCount를 metrics로 전달한다")
  void 스텝_실행_후_메트릭스가_기록된다() {

    // given
    LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
    LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

    given(stepExecution.getStartTime()).willReturn(start);
    given(stepExecution.getEndTime()).willReturn(end);
    given(stepExecution.getWriteCount()).willReturn(123L);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(commentCleanupMetrics).should().countDeleted(123);
    then(commentCleanupMetrics).should().recordStepDuration(Duration.ofSeconds(5));
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }
}
