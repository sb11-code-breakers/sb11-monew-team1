package com.sprint.mission.monew.batch.service;

import com.sprint.mission.monew.batch.exception.NotificationCleanupJobFailedException;
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
public class NotificationCleanupService {

  private final JobLauncher jobLauncher;

  @Qualifier("notificationCleanupJob")
  private final Job notificationCleanupJob;

  public void executeCleanup() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();
      jobLauncher.run(notificationCleanupJob, params);
    } catch (Exception e) {
      throw NotificationCleanupJobFailedException.wrap(e);
    }
  }
}
