package com.sprint.mission.monew.batch.news.collect.service;

import com.sprint.mission.monew.batch.news.collect.exception.NewsCollectJobFailedException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsCollectService {

  private final JobLauncher jobLauncher;

  @Qualifier("newsCollectJob")
  private final Job newsCollectJob;

  public void executeCollect() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();
      jobLauncher.run(newsCollectJob, params);
    } catch (Exception e) {
      throw NewsCollectJobFailedException.wrap(e);
    }
  }
}
