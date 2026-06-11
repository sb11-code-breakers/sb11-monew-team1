package com.sprint.mission.monew.batch.news.collect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.monew.batch.news.collect.exception.NewsCollectJobFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class NewsCollectServiceTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job newsCollectJob;

  @InjectMocks
  private NewsCollectService newsCollectService;

  @Test
  @DisplayName("Job 실행 실패 시 NewsCollectJobFailedException으로 감싼다")
  void newsCollectJob_실행중_NewsCollectJobFailedException_예외_발생() throws Exception {

    // given
    when(jobLauncher.run(eq(newsCollectJob), any(JobParameters.class)))
        .thenThrow(new RuntimeException("batch fail"));

    // when & then
    assertThatThrownBy(() -> newsCollectService.executeCollect())
        .isInstanceOf(NewsCollectJobFailedException.class);
  }

  @Test
  @DisplayName("newsCollectJob이 JobLauncher를 통해 정상 실행된다")
  void newsCollectJob이_JobLauncher를_통해_정상_실행된다() throws Exception {

    // given
    when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .thenReturn(null);

    // when
    newsCollectService.executeCollect();

    // then
    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobLauncher, times(1)).run(eq(newsCollectJob), paramsCaptor.capture());
    assertThat(paramsCaptor.getValue().getParameters()).containsKey("time");
  }
}