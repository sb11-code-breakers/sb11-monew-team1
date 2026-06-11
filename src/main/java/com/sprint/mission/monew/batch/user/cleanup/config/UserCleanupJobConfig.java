package com.sprint.mission.monew.batch.user.cleanup.config;

import com.sprint.mission.monew.batch.user.cleanup.dto.UserCleanupItem;
import com.sprint.mission.monew.batch.user.cleanup.listener.UserCleanupJobListener;
import com.sprint.mission.monew.batch.user.cleanup.reader.UserCleanupReader;
import com.sprint.mission.monew.batch.user.cleanup.listener.UserCleanupStepListener;
import com.sprint.mission.monew.batch.user.cleanup.writer.UserCleanupWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class UserCleanupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final UserCleanupJobListener userCleanupJobListener;
  private final UserCleanupReader userCleanupReader;
  private final UserCleanupWriter userCleanupWriter;
  private final UserCleanupStepListener userCleanupStepListener;

  @Value("${batch.user-cleanup.chunk-size}")
  private int chunkSize;

  @Bean(name = "userCleanupJob")
  public Job userCleanupJob() {
    return new JobBuilder("userCleanupJob", jobRepository)
        .listener(userCleanupJobListener)
        .start(userCleanupStep()).build();
  }

  @Bean
  public Step userCleanupStep() {
    return new StepBuilder("userCleanupStep", jobRepository)
        .<UserCleanupItem, UserCleanupItem>chunk(chunkSize, transactionManager)
        .reader(userCleanupReader)
        .writer(userCleanupWriter)
        .listener(userCleanupStepListener)
        .build();
  }

}
