package com.sprint.mission.monew.batch.jobconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.batch.listener.CommentCleanupStepListener;
import com.sprint.mission.monew.batch.reader.CommentCleanupReader;
import com.sprint.mission.monew.batch.writer.CommentCleanupWriter;
import com.sprint.mission.monew.batch.config.CommentCleanupJobConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

public class CommentCleanupJobConfigTest {

  private final JobRepository jobRepository = mock(JobRepository.class);
  private final PlatformTransactionManager transactionManager = mock(
      PlatformTransactionManager.class);

  @Nested
  @DisplayName("CommentCleanupJobConfig Job, Step 테스트")
  class JobStepTest {

    @Test
    @DisplayName("Job, Step 생성 성공")
    void job_step_생성_성공() throws Exception {
      // given
      CommentCleanupReader reader = mock(CommentCleanupReader.class);
      CommentCleanupWriter writer = mock(CommentCleanupWriter.class);
      CommentCleanupStepListener listener = mock(CommentCleanupStepListener.class);

      CommentCleanupJobConfig config = new CommentCleanupJobConfig(
          jobRepository,
          transactionManager,
          reader,
          writer,
          listener
      );

      var field = CommentCleanupJobConfig.class.getDeclaredField("chunkSize");
      field.setAccessible(true);
      field.set(config, 1000);

      // when
      Job job = config.commentCleanupJob();
      Step step = config.commentCleanupStep();

      // then
      assertNotNull(job);
      assertThat(job.getName()).isEqualTo("commentCleanupJob");

      assertNotNull(step);
      assertThat(step.getName()).isEqualTo("commentCleanupStep");
    }
  }
}
