package com.sprint.mission.monew.batch.comment.cleanup.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.monew.batch.comment.cleanup.metrics.CommentCleanupMetrics;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;

@ExtendWith(MockitoExtension.class)
public class CommentCleanupJobListenerTest {

  @Mock
  CommentCleanupMetrics commentCleanupMetrics;

  @InjectMocks
  CommentCleanupJobListener listener;

  JobExecution jobExecution;

  @BeforeEach
  void setUp() {
    jobExecution = mock(JobExecution.class);
  }

  @Nested
  @DisplayName("beforeJob 테스트")
  class BeforeJob {

    @Test
    @DisplayName("beforeJob은 실행되어야 한다")
    void before_job_실행여부_확인() {
      // given
      when(jobExecution.getId()).thenReturn(1L);
      when(jobExecution.getJobParameters()).thenReturn(mock(JobParameters.class));

      // when
      listener.beforeJob(jobExecution);

      // then
      verify(jobExecution).getId();
      verify(jobExecution).getJobParameters();
    }
  }

  @Nested
  @DisplayName("afterJob 테스트")
  class AfterJob {

    @Test
    @DisplayName("시작 시간 정보 없으면 metrics 호출 없이 종료한다")
    void 시간_null이면_metrics_미호출() {
      // given
      LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

      given(jobExecution.getStartTime()).willReturn(null);
      given(jobExecution.getEndTime()).willReturn(end);
      given(jobExecution.getStatus()).willReturn(BatchStatus.COMPLETED);

      // when
      listener.afterJob(jobExecution);

      // then
      then(commentCleanupMetrics).should(never()).recordJobDuration(any(Duration.class));
    }

    @Test
    @DisplayName("완료 시간 정보 없으면 metrics 호출 없이 종료한다")
    void 완료_시간_null이면_metrics_미호출() {
      // given
      LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);

      given(jobExecution.getStartTime()).willReturn(start);
      given(jobExecution.getEndTime()).willReturn(null);
      given(jobExecution.getStatus()).willReturn(BatchStatus.COMPLETED);

      // when
      listener.afterJob(jobExecution);

      // then
      then(commentCleanupMetrics).should(never()).recordJobDuration(any(Duration.class));
    }

    @Test
    @DisplayName("FailureException 존재 시 로그 처리 로직이 실행된다")
    void job이_실패하여_FailureException_있으면_warn로그_실행() {
      // given
      when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

      when(jobExecution.getAllFailureExceptions())
          .thenReturn(List.of(new RuntimeException("예외 발생")));

      // when
      listener.afterJob(jobExecution);

      // then
      verify(commentCleanupMetrics, never()).markSuccess();
    }

    @Test
    @DisplayName("Job 실패 시 markSuccess는 호출되지 않는다")
    void job_실패하면_markSuccess_미호출() {
      // given
      when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);
      when(jobExecution.getAllFailureExceptions()).thenReturn(
          List.of(new RuntimeException("예외 발생")));

      // when
      listener.afterJob(jobExecution);

      // then
      verify(commentCleanupMetrics, never()).markSuccess();
    }

    @Test
    @DisplayName("Job 상태가 COMPLETED도 FAILED도 아닌 상태에서도 markSuccess는 호출되지 않는다")
    void job_기타_상태에도_markSuccess_미호출() {
      when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);
      when(jobExecution.getAllFailureExceptions()).thenReturn(List.of());

      listener.afterJob(jobExecution);

      verify(commentCleanupMetrics, never()).markSuccess();
    }

    @Test
    @DisplayName("Job 성공 시 markSuccess가 호출되고, Duration이 계산된다")
    void job_성공하면_markSuccess_호출_Duration_계산() {
      // given
      when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

      LocalDateTime start = LocalDateTime.of(2026, 6, 10, 10, 0, 0);
      LocalDateTime end = LocalDateTime.of(2026, 6, 10, 10, 0, 5);

      when(jobExecution.getStartTime()).thenReturn(start);
      when(jobExecution.getEndTime()).thenReturn(end);

      // when
      listener.afterJob(jobExecution);

      // then
      verify(commentCleanupMetrics, times(1)).markSuccess();
      verify(commentCleanupMetrics).recordJobDuration(Duration.ofSeconds(5));
    }
  }
}
