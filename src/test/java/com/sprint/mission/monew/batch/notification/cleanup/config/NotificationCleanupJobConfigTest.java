package com.sprint.mission.monew.batch.notification.cleanup.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.batch.notification.cleanup.listener.NotificationCleanupStepListener;
import com.sprint.mission.monew.batch.notification.cleanup.reader.NotificationCleanupReader;
import com.sprint.mission.monew.batch.notification.cleanup.writer.NotificationCleanupWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class NotificationCleanupJobConfigTest {

  private final JobRepository jobRepository = mock(JobRepository.class);
  private final PlatformTransactionManager transactionManager = mock(
      PlatformTransactionManager.class);

  @Nested
  @DisplayName("NotificationCleanupJobConfig Job, Step 테스트")
  class JobStepTest {

    @Test
    @DisplayName("Job, Step 생성 성공")
    void job_step_생성_성공() throws Exception {
      // given
      NotificationCleanupReader reader = mock(NotificationCleanupReader.class);
      NotificationCleanupWriter writer = mock(NotificationCleanupWriter.class);
      NotificationCleanupStepListener listener = mock(NotificationCleanupStepListener.class);

      NotificationCleanupJobConfig config = new NotificationCleanupJobConfig(
          jobRepository,
          transactionManager,
          reader,
          writer,
          listener
      );

      var field = NotificationCleanupJobConfig.class.getDeclaredField("chunkSize");
      field.setAccessible(true);
      field.set(config, 1000);

      // when
      Job job = config.notificationCleanupJob();
      Step step = config.notificationCleanupStep();

      // then
      assertNotNull(job);
      assertThat(job.getName()).isEqualTo("notificationCleanupJob");

      assertNotNull(step);
      assertThat(step.getName()).isEqualTo("notificationCleanupStep");
    }
  }
}