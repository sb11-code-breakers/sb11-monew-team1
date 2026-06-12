package com.sprint.mission.monew.batch.news.collect.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.sprint.mission.monew.batch.news.collect.metrics.NewsCollectMetrics;
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
public class NewsCollectStepListenerTest {

  @Mock
  NewsCollectMetrics newsCollectMetrics;

  @InjectMocks
  NewsCollectStepListener listener;

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
    then(newsCollectMetrics).shouldHaveNoInteractions();
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
    then(newsCollectMetrics).shouldHaveNoInteractions();
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("Step이 실패해도 metrics는 기록되고 ExitStatus는 그대로 반환된다")
  void 실패해도_메트릭은_기록된다() {

    // given
    LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
    LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

    given(stepExecution.getStartTime()).willReturn(start);
    given(stepExecution.getEndTime()).willReturn(end);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.FAILED);

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(newsCollectMetrics)
        .should()
        .recordStepDuration(Duration.ofSeconds(5));

    assertThat(result).isEqualTo(ExitStatus.FAILED);
  }

  @Test
  @DisplayName("metrics에서 예외가 발생해도 Step은 정상 종료된다")
  void 메트릭_예외가_발생해도_step은_정상_종료된다() {

    // given
    LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
    LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

    given(stepExecution.getStartTime()).willReturn(start);
    given(stepExecution.getEndTime()).willReturn(end);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    doThrow(new RuntimeException("metrics fail"))
        .when(newsCollectMetrics)
        .recordStepDuration(Duration.ofSeconds(5));

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(newsCollectMetrics).should().recordStepDuration(Duration.ofSeconds(5));
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }

  @Test
  @DisplayName("StepExecution recordCollectDuration를 metrics로 전달한다")
  void 스텝_실행_후_메트릭스가_기록된다() {

    // given
    LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
    LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

    given(stepExecution.getStartTime()).willReturn(start);
    given(stepExecution.getEndTime()).willReturn(end);
    given(stepExecution.getExitStatus()).willReturn(ExitStatus.COMPLETED);

    // when
    ExitStatus result = listener.afterStep(stepExecution);

    // then
    then(newsCollectMetrics).should().recordStepDuration(Duration.ofSeconds(5));
    assertThat(result).isEqualTo(ExitStatus.COMPLETED);
  }
}
