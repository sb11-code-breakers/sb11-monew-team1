package com.sprint.mission.monew.batch.article.backup.config;

import com.sprint.mission.monew.batch.article.backup.dto.ArticleBackupItem;
import com.sprint.mission.monew.batch.article.backup.listener.ArticleBackupStepListener;
import com.sprint.mission.monew.batch.article.backup.reader.ArticleBackupReader;
import com.sprint.mission.monew.batch.article.backup.writer.ArticleBackupWriter;
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
public class ArticleBackupJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final ArticleBackupReader articleBackupReader;
  private final ArticleBackupWriter articleBackupWriter;
  private final ArticleBackupStepListener articleBackupStepListener;

  @Value("${batch.article-backup.chunk-size}")
  private int chunkSize;

  @Bean(name = "articleBackupJob")
  public Job articleBackupJob() {
    return new JobBuilder("articleBackupJob", jobRepository)
        .start(articleBackupStep())
        .build();
  }

  @Bean
  public Step articleBackupStep() {
    return new StepBuilder("articleBackupStep", jobRepository)
        .<ArticleBackupItem, ArticleBackupItem>chunk(chunkSize, transactionManager)
        .reader(articleBackupReader)
        .writer(articleBackupWriter)
        .listener(articleBackupStepListener)
        .build();
  }
}