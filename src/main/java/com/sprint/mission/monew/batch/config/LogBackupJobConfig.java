package com.sprint.mission.monew.batch.config;

import com.sprint.mission.monew.batch.dto.LogContent;
import com.sprint.mission.monew.batch.dto.UploadPayload;
import com.sprint.mission.monew.batch.processor.LogBackupProcessor;
import com.sprint.mission.monew.batch.reader.LogBackupReader;
import com.sprint.mission.monew.batch.writer.LogBackupWriter;
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
public class LogBackupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final LogBackupReader logBackupReader;
  private final LogBackupProcessor logBackupProcessor;
  private final LogBackupWriter logBackupWriter;

  @Value("${batch.log-backup.chunk-size}")
  private int chunkSize;

  @Bean(name = "logBackupJob")
  public Job logBackupJob() {
    return new JobBuilder("logBackupJob", jobRepository)
        .start(logBackupStep())
        .build();
  }

  @Bean
  public Step logBackupStep() {
    return new StepBuilder("logBackupStep", jobRepository)
        .<LogContent, UploadPayload>chunk(chunkSize, transactionManager)
        .reader(logBackupReader)
        .processor(logBackupProcessor)
        .writer(logBackupWriter)
        .build();
  }
}
