package com.sprint.mission.monew.common.config;

import com.sprint.mission.monew.batch.dto.NewsCollectItem;
import com.sprint.mission.monew.batch.reader.NewsCollectReader;
import com.sprint.mission.monew.batch.writer.NewsCollectWriter;
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
public class NewsCollectJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  private final NewsCollectReader newsCollectReader;
  private final NewsCollectWriter newsCollectWriter;

  @Value("${batch.news-collect.chunk-size}")
  private int chunkSize;

  @Bean(name = "newsCollectJob")
  public Job newsCollectJob() {
    return new JobBuilder("newsCollectJob", jobRepository)
        .start(newsCollectStep()).build();
  }

  @Bean
  public Step newsCollectStep() {
    return new StepBuilder("newsCollectStep", jobRepository)
        .<NewsCollectItem, NewsCollectItem>chunk(chunkSize, transactionManager)
        .reader(newsCollectReader)
        .writer(newsCollectWriter)
        .build();
  }
}
