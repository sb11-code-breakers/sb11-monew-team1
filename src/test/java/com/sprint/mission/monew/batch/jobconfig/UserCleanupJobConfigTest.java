package com.sprint.mission.monew.batch.jobconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.batch.listener.UserCleanupStepListener;
import com.sprint.mission.monew.batch.reader.UserCleanupReader;
import com.sprint.mission.monew.batch.writer.UserCleanupWriter;
import com.sprint.mission.monew.batch.config.UserCleanupJobConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class UserCleanupJobConfigTest {

  private final JobRepository jobRepository = mock(JobRepository.class);
  private final PlatformTransactionManager transactionManager = mock(
      PlatformTransactionManager.class);

  @Nested
  @DisplayName("UserCleanupJobConfig Job, Step 테스트")
  class JobStepTest {

    @Test
    @DisplayName("Job, Step 생성 성공")
    void job_step_생성_성공() throws Exception {
      // given
      UserCleanupReader reader = mock(UserCleanupReader.class);
      UserCleanupWriter writer = mock(UserCleanupWriter.class);
      UserCleanupStepListener listener = mock(UserCleanupStepListener.class);

      UserCleanupJobConfig config = new UserCleanupJobConfig(
          jobRepository,
          transactionManager,
          reader,
          writer,
          listener
      );

      var field = UserCleanupJobConfig.class.getDeclaredField("chunkSize");
      field.setAccessible(true);
      field.set(config, 1000);

      // when
      Job job = config.userCleanupJob();
      Step step = config.userCleanupStep();

      // then
      assertNotNull(job);
      assertThat(job.getName()).isEqualTo("userCleanupJob");

      assertNotNull(step);
      assertThat(step.getName()).isEqualTo("userCleanupStep");
    }
  }
}