package com.sprint.mission.monew.batch.article.backup.service;

import com.sprint.mission.monew.batch.article.backup.exception.ArticleBackupJobFailedException;
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
public class ArticleBackupService {

  private final JobLauncher jobLauncher;

  @Qualifier("articleBackupJob")
  private final Job articleBackupJob;

  public void executeBackup() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();
      jobLauncher.run(articleBackupJob, params);
    } catch (Exception e) {
      throw ArticleBackupJobFailedException.wrap(e);
    }
  }
}