package com.sprint.mission.monew.batch.jobconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.sprint.mission.monew.batch.reader.NewsCollectReader;
import com.sprint.mission.monew.batch.writer.NewsCollectWriter;
import com.sprint.mission.monew.batch.config.NewsCollectJobConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

class NewsCollectJobConfigTest {

  private final JobRepository jobRepository = mock(JobRepository.class);
  private final PlatformTransactionManager transactionManager = mock(
      PlatformTransactionManager.class);

  @Nested
  @DisplayName("NewsCollectJobConfig Job, Step 테스트")
  class JobStepTest {

    @Test
    @DisplayName("Job, Step 생성 성공")
    void job_step_생성_성공() throws Exception {
      // given
      NewsCollectReader reader = mock(NewsCollectReader.class);
      NewsCollectWriter writer = mock(NewsCollectWriter.class);

      NewsCollectJobConfig config = new NewsCollectJobConfig(
          jobRepository,
          transactionManager,
          reader,
          writer
      );

      var field = NewsCollectJobConfig.class.getDeclaredField("chunkSize");
      field.setAccessible(true);
      field.set(config, 1000);

      // when
      Job job = config.newsCollectJob();
      Step step = config.newsCollectStep();

      // then
      assertNotNull(job);
      assertThat(job.getName()).isEqualTo("newsCollectJob");

      assertNotNull(step);
      assertThat(step.getName()).isEqualTo("newsCollectStep");
    }
  }
}