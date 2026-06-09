package com.sprint.mission.monew.batch.config;

import com.sprint.mission.monew.batch.dto.CommentCleanupItem;
import com.sprint.mission.monew.batch.listener.CommentCleanupStepListener;
import com.sprint.mission.monew.batch.reader.CommentCleanupReader;
import com.sprint.mission.monew.batch.writer.CommentCleanupWriter;
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
public class CommentCleanupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final CommentCleanupReader commentCleanupReader;
  private final CommentCleanupWriter commentCleanupWriter;
  private final CommentCleanupStepListener commentCleanupStepListener;

  @Value("${batch.comment-cleanup.chunk-size}")
  private int chunkSize;

  @Bean(name = "commentCleanupJob")
  public Job commentCleanupJob() {
    return new JobBuilder("commentCleanupJob", jobRepository)
        .start(commentCleanupStep()).build();
  }

  @Bean
  public Step commentCleanupStep() {
    return new StepBuilder("commentCleanupStep", jobRepository)
        .<CommentCleanupItem, CommentCleanupItem>chunk(chunkSize, transactionManager)
        .reader(commentCleanupReader)
        .writer(commentCleanupWriter)
        .listener(commentCleanupStepListener)
        .build();
  }

}
