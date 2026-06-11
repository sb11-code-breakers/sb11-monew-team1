package com.sprint.mission.monew.batch.article.backup.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.batch.article.backup.listener.ArticleBackupStepListener;
import com.sprint.mission.monew.batch.article.backup.reader.ArticleBackupReader;
import com.sprint.mission.monew.batch.article.backup.writer.ArticleBackupWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class ArticleBackupJobConfigTest {

  private final JobRepository jobRepository = mock(JobRepository.class);
  private final PlatformTransactionManager transactionManager = mock(
      PlatformTransactionManager.class);

  @Nested
  @DisplayName("ArticleBackupJobConfig Job, Step 테스트")
  class JobStepTest {

    @Test
    @DisplayName("Job, Step 생성 성공")
    void job_step_생성_성공() {
      // given
      ArticleBackupReader reader = mock(ArticleBackupReader.class);
      ArticleBackupWriter writer = mock(ArticleBackupWriter.class);
      ArticleBackupStepListener listener = mock(ArticleBackupStepListener.class);

      ArticleBackupJobConfig config = new ArticleBackupJobConfig(
          jobRepository,
          transactionManager,
          reader,
          writer,
          listener
      );

      // when
      Job job = config.articleBackupJob();
      Step step = config.articleBackupStep();

      // then
      assertNotNull(job);
      assertThat(job.getName()).isEqualTo("articleBackupJob");

      assertNotNull(step);
      assertThat(step.getName()).isEqualTo("articleBackupStep");
    }
  }
}