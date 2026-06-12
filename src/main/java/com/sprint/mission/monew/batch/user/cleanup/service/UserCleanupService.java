package com.sprint.mission.monew.batch.user.cleanup.service;

import com.sprint.mission.monew.batch.user.cleanup.exception.UserCleanupJobFailedException;
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
public class UserCleanupService {

  private final JobLauncher jobLauncher;

  @Qualifier("userCleanupJob")
  private final Job userCleanupJob;

  public void executeCleanup() {
    try {
      JobParameters params = new JobParametersBuilder()
          .addLong("time", Instant.now().toEpochMilli())
          .toJobParameters();
      jobLauncher.run(userCleanupJob, params);
    } catch (Exception e) {
      throw UserCleanupJobFailedException.wrap(e);
    }
  }
}
