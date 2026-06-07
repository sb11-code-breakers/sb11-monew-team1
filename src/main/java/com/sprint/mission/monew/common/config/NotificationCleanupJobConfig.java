package com.sprint.mission.monew.common.config;

import com.sprint.mission.monew.batch.dto.NotificationCleanupItem;
import com.sprint.mission.monew.batch.listener.NotificationCleanupStepListener;
import com.sprint.mission.monew.batch.reader.NotificationCleanupReader;
import com.sprint.mission.monew.batch.writer.NotificationCleanupWriter;
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
public class NotificationCleanupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final NotificationCleanupReader notificationCleanupReader;
  private final NotificationCleanupWriter notificationCleanupWriter;
  private final NotificationCleanupStepListener notificationCleanupStepListener;

  @Value("${batch.notification-cleanup.chunk-size}")
  private int chunkSize;

  @Bean(name = "notificationCleanupJob")
  public Job notificationCleanupJob() {
    return new JobBuilder("notificationCleanupJob", jobRepository)
        .start(notificationCleanupStep()).build();
  }

  @Bean
  public Step notificationCleanupStep() {
    return new StepBuilder("notificationCleanupStep", jobRepository)
        .<NotificationCleanupItem, NotificationCleanupItem>chunk(chunkSize, transactionManager)
        .reader(notificationCleanupReader)
        .writer(notificationCleanupWriter)
        .listener(notificationCleanupStepListener)
        .build();
  }

}
