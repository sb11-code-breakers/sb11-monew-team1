package com.sprint.mission.monew.batch.log.backup.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.batch.log.backup.listener.LogBackupStepListener;
import com.sprint.mission.monew.batch.log.backup.processor.LogBackupProcessor;
import com.sprint.mission.monew.batch.log.backup.reader.LogBackupReader;
import com.sprint.mission.monew.batch.log.backup.writer.LogBackupWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class LogBackupJobConfigTest {

  private final JobRepository jobRepository = mock(JobRepository.class);
  private final PlatformTransactionManager transactionManager = mock(
      PlatformTransactionManager.class);

  @Nested
  @DisplayName("LogBackupJobConfig Job, Step 테스트")
  class JobStepTest {

    @Test
    @DisplayName("Job, Step 생성 성공")
    void job_step_생성_성공() throws Exception {
      // given
      LogBackupReader reader = mock(LogBackupReader.class);
      LogBackupProcessor processor = mock(LogBackupProcessor.class);
      LogBackupWriter writer = mock(LogBackupWriter.class);
      LogBackupStepListener listener = mock(LogBackupStepListener.class);

      LogBackupJobConfig config = new LogBackupJobConfig(
          jobRepository,
          transactionManager,
          reader,
          processor,
          writer,
          listener
      );

      // when
      Job job = config.logBackupJob();
      Step step = config.logBackupStep();

      // then
      assertNotNull(job);
      assertThat(job.getName()).isEqualTo("logBackupJob");

      assertNotNull(step);
      assertThat(step.getName()).isEqualTo("logBackupStep");
    }
  }
}