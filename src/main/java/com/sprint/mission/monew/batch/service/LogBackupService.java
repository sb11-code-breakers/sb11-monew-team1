package com.sprint.mission.monew.batch.service;

import com.sprint.mission.monew.batch.exception.LogBackupJobFailedException;
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
public class LogBackupService {

  private final JobLauncher jobLauncher;

  @Qualifier("logBackupJob")
  private final Job logBackupJob;

  public void executeBackup() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();
      jobLauncher.run(logBackupJob, params);
    } catch (Exception e) {
      throw LogBackupJobFailedException.wrap(e);
    }
  }
}