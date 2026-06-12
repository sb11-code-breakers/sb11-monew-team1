package com.sprint.mission.monew.batch.article.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sprint.mission.monew.batch.article.backup.exception.ArticleBackupJobFailedException;
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
class ArticleBackupServiceTest {

  @Mock
  private JobLauncher jobLauncher;

  @Mock
  private Job articleBackupJob;

  @InjectMocks
  private ArticleBackupService articleBackupService;

  @Test
  @DisplayName("Job 실행 실패 시 ArticleBackupJobFailedException으로 감싼다")
  void articleBackupJob_실행중_ArticleBackupJobFailedException_예외_발생() throws Exception {
    // given
    when(jobLauncher.run(eq(articleBackupJob), any(JobParameters.class)))
        .thenThrow(new RuntimeException("batch fail"));

    // when & then
    assertThatThrownBy(() -> articleBackupService.executeBackup())
        .isInstanceOf(ArticleBackupJobFailedException.class);
  }

  @Test
  @DisplayName("articleBackupJob이 JobLauncher를 통해 정상 실행된다")
  void articleBackupJob이_JobLauncher를_통해_정상_실행된다() throws Exception {
    // given
    when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
        .thenReturn(null);

    // when
    articleBackupService.executeBackup();

    // then
    ArgumentCaptor<JobParameters> paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobLauncher, times(1)).run(eq(articleBackupJob), paramsCaptor.capture());
    assertThat(paramsCaptor.getValue().getParameters()).containsKey("time");
  }
}